# Hotel Management and Reservation System — Java Proof of Concept

A guest and administration web app in front of three Spring Boot microservices
(hotel management, shop, payment). Each service is built with clean architecture and owns its
own PostgreSQL database. The code demonstrates six design patterns from the UML design:
Strategy, Decorator, State, Observer, Adapter, and Facade.

## Requirements
- Docker with Compose, to run everything
- JDK 21 and Maven 3.9, to build and test locally (IntelliJ's bundled Maven works)

## Run with Docker Compose

Create the passwords once (they go to `secrets/`, which is git-ignored):

```bash
./scripts/generate-secrets.sh
```

Build and start everything:

```bash
docker compose up --build
```

Open the web app at **http://localhost:8090**.

- **Guests** search availability by dates, then (after logging in or registering) pick a room,
  add extras, review the price and pay by card. They can cancel a confirmed reservation from
  "My reservations" and get a refund.
- **Administrators** are pre-created in the hotel database at startup. Log in as `admin` with the
  password in `secrets/admin_password.txt` to see every reservation and room, and to check guests
  in and out.

| Container | Port | Purpose |
|---|---|---|
| `web-app` | 8090 | Guest and administration web interface |
| `hotel-management-service` | 8080 | Rooms, accounts, reservations (REST API) |
| `shop-service` | 8081 | Pricing and additional services (REST API) |
| `payment-service` | 8082 | Payments (REST API) |
| `hotel-db`, `shop-db`, `payment-db` | not published | One PostgreSQL database per service |

Data is kept in Docker volumes. To start from empty databases: `docker compose down -v`.

## Booking, availability and payments

**Booking flow (web app):** search dates → only free rooms are listed → choose a room (log in
or register if needed, then you come back to it) → pick extras → review the shop's price
breakdown → pay by card → confirmation. A declined card brings you back to the review page with
the reason, and nothing is booked.

**No overlapping bookings:**
- Dates are checked first: no check-in in the past, check-out after check-in, at most 30 nights.
- Availability is date-based: a room is taken for a night only by a reservation that holds it
  (pending while being paid, confirmed, or checked in). Cancelled and completed stays free their
  nights.
- The hotel database has a `room_nights` table with one row per room per night and a primary key
  on (room, night). A reservation that holds a room owns those rows, so the database itself
  rejects a second booking of the same night, even when two bookings arrive at the same moment.
- The room is held before the card is charged, so a refused booking is never charged.

**Simulated payment system:** the payment service charges cards through a simulated card network
("PayFlow", behind the Adapter). It validates the card (Luhn checksum, expiry, CVC), then the
card number decides the outcome:

| Test card | Result |
|---|---|
| `4242 4242 4242 4242` (or any other valid number) | Approved |
| `4000 0000 0000 0002` | Declined |
| `4000 0000 0000 9995` | Insufficient funds |
| `4000 0000 0000 0069` | Expired card |

Use any future expiry date and any 3-digit CVC. Only the card brand and last four digits are
stored; card numbers and CVCs are never written to a database or a log. Cancelling a confirmed
reservation refunds its payment.

## Security
- **Web app:** form login with sessions and CSRF protection. Booking pages need the `GUEST` role,
  administration pages need `ADMIN`. The web app checks usernames and passwords with the hotel
  management service and books under the signed-in guest's account, never an id from the form.
- **Accounts:** guests register through the web app; passwords are stored as bcrypt hashes. The
  administrator is created (or its password updated) from the `admin_password` secret when the
  hotel management service starts. The service also refuses bookings for unknown guest accounts.
- **Service APIs:** every REST API needs HTTP Basic credentials. The web app holds the hotel
  management password; hotel management holds the shop and payment passwords.
- **Secrets:** every password (APIs, databases, admin) is a Docker secret mounted at
  `/run/secrets/<name>` and read by Spring through `configtree:`. Each container gets only the
  secrets it needs. Services refuse to start without their API password.
- To rotate a password: delete its file in `secrets/`, run `./scripts/generate-secrets.sh`,
  then `docker compose up -d --force-recreate`. (Database passwords are set when a database is
  first created; rotating one also needs `docker compose down -v`, which deletes the data.)

## Service APIs

All routes need HTTP Basic credentials (`401` without them).

| Route | Success | Errors |
|---|---|---|
| `GET /api/rooms` (hotel) | `200` rooms with status | |
| `GET /api/rooms/available?checkIn=&checkOut=` (hotel) | `200` rooms free for every night | `400` invalid dates |
| `GET /api/reservations[?guestId=]` (hotel) | `200` all reservations, or one guest's | |
| `POST /api/reservations/quote` (hotel) | `200` total and breakdown for a free room | `400` invalid dates, `404` unknown room, `409` room taken |
| `POST /api/reservations` (hotel, with card) | `201` confirmed reservation with payment reference | `400` invalid, `402` card declined, `404` unknown guest or room, `409` room booked for those dates |
| `POST /api/reservations/{id}/cancel` | `200` cancelled and refunded | `404` unknown (or another guest's), `409` wrong state |
| `POST /api/reservations/{id}/check-in` | `200` reservation | `404` unknown, `409` wrong state |
| `POST /api/reservations/{id}/check-out` | `200` reservation | `404` unknown, `409` wrong state |
| `POST /api/accounts` (hotel) | `201` guest account | `400` invalid, `409` username taken |
| `POST /api/accounts/authenticate` (hotel) | `200` account with role | `401` wrong username or password |
| `POST /api/shop/quotes` (shop) | `200` total, currency, breakdown | `400` invalid |
| `POST /api/payments` (payment, with card) | `200` status `APPROVED` or `DECLINED` with reason | `400` invalid |
| `POST /api/payments/{transactionId}/refund` | `200` status `REFUNDED` | `404` unknown, `409` not refundable |

## Tests

```bash
mvn verify
```

The same command runs on every push and pull request to `main` (`.github/workflows/tests.yml`).
Tests use an in-memory H2 database in PostgreSQL mode with the real Flyway migrations, so no
Docker is needed.

| Test | Covers |
|---|---|
| Use-case tests (`ReservationUseCasesTest`, `AccountUseCasesTest`, `QuoteStayInteractorTest`, `ProcessPaymentInteractorTest`) | Business rules, with recording presenters and in-memory or stub gateways |
| API tests (`ReservationApiTest`, `ShopApiTest`, `PaymentApiTest`) | Routes, status codes and auth, through Spring and the database |
| `PersistenceAdaptersTest` | The JPA gateway adapters against the real schema, including the database refusing two holds on the same night |
| `PaymentCardTest` | Card validation (Luhn, expiry, CVC), brands, and that card numbers are never printed |
| `ServiceClientsTest` | The exact HTTP calls hotel management makes to shop and payment |
| `WebAppSecurityTest` | Pages, role rules, CSRF, login and registration in the web app |
| `ArchitectureTest` (every module) | Layer rules, no imports between services, no frameworks in the core layers |

## Project layout

| Module | Business purpose | Database |
|---|---|---|
| `web-app` | Guest and administration web interface | none |
| `hotel-management-service` | Rooms, guest and admin accounts, reservations and their lifecycle, notifications | `hotel` |
| `shop-service` | Pricing strategies and the additional services price list | `shop` |
| `payment-service` | Charging the guest and recording every payment | `payment` |

Each service is organised with clean architecture (`controller`, `presenter`, `model`, `usecase`,
`entity`, `gateway`, `infrastructure`, `config`); the database is one more adapter in
`infrastructure`. See [docs/architecture.md](docs/architecture.md) for diagrams and rules.

## What is intentionally simplified
- The payment gateway is simulated; no real payment is taken.
- Three rooms are seeded by a database migration; there is no room administration yet.
- Auth between containers is HTTP Basic over plain HTTP; put HTTPS in front for anything real.
- Web sessions are kept in memory, so restarting the web app logs everyone out.

## Pattern map
| Pattern | Service | Main implementation |
|---|---|---|
| Strategy | shop | `PricingStrategy`, `StandardPricing`, `SeasonalPricing`, `LongStayDiscountPricing` |
| Decorator | shop | `Chargeable`, `RoomStayCharge`, `ServiceDecorator`, service decorators |
| State | hotel management | `ReservationState` and concrete reservation states (also decides which states hold a room) |
| Observer | hotel management | `ReservationObserver`, `ReservationEventPublisher`, notifier classes |
| Adapter | payment | `PaymentProcessor`, `PayFlowGatewayAdapter`, `PayFlowGateway` |
| Facade | hotel management | `FrontDeskController` |

## Try changing the demo
- In `shop-service/.../config/ShopConfig.java`, replace `SeasonalPricing` with `StandardPricing` or `LongStayDiscountPricing`.
- Tick or untick breakfast, spa or airport transfer when booking to add/remove `BreakfastService`, `SpaPackage`, or `AirportTransfer`.
- Change a price in the shop database's `extra_prices` table and book again.
- Pay with `4000 0000 0000 9995` to see a declined card, then with `4242 4242 4242 4242`.
- Book a room, then search overlapping dates: the room is no longer listed.
# asd-hotel-reservation-nonaislop
