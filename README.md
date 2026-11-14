# Hotel Management and Reservation System — Java Proof of Concept

Three Spring Boot microservices (hotel management, shop, payment), each built with clean
architecture, demonstrating six design patterns from the UML design:
Strategy, Decorator, State, Observer, Adapter, and Facade.

## Requirements
- Docker with Compose, to run the services
- JDK 21 and Maven 3.9, to build and test locally (IntelliJ's bundled Maven works)

## Run with Docker Compose

Create the API passwords once (they go to `secrets/`, which is git-ignored):

```bash
./scripts/generate-secrets.sh
```

Build and start the three services:

```bash
docker compose up --build
```

| Service | Port | API user | Password secret |
|---|---|---|---|
| `hotel-management-service` | 8080 | `front-desk` | `secrets/hotel_api_password.txt` |
| `shop-service` | 8081 | `shop-client` | `secrets/shop_api_password.txt` |
| `payment-service` | 8082 | `payment-client` | `secrets/payment_api_password.txt` |

## Try the API

```bash
HOTEL_PW=$(cat secrets/hotel_api_password.txt)
```

Book room 204 (the hotel service asks the shop for the price and the payment service to charge):

```bash
curl -u "front-desk:$HOTEL_PW" -H 'Content-Type: application/json' -X POST http://localhost:8080/api/reservations -d '{"guest":{"id":"G-01","name":"Alex Morgan","email":"alex@example.com","phone":"+40123456789"},"roomNumber":"204","checkIn":"2026-12-10","checkOut":"2026-12-13","breakfast":true,"spa":true,"airportTransfer":false}'
```

Check in and check out (use the `reservationId` from the booking response):

```bash
curl -u "front-desk:$HOTEL_PW" -X POST http://localhost:8080/api/reservations/RES-1000/check-in
```

```bash
curl -u "front-desk:$HOTEL_PW" -X POST http://localhost:8080/api/reservations/RES-1000/check-out
```

| Route | Success | Errors |
|---|---|---|
| `POST /api/reservations` (hotel) | `201` reservation (`CONFIRMED`, or `PENDING` if payment was declined) | `400` invalid request, `404` unknown room, `409` room not available |
| `POST /api/reservations/{id}/check-in` | `200` reservation | `404` unknown reservation, `409` wrong state |
| `POST /api/reservations/{id}/check-out` | `200` reservation | `404` unknown reservation, `409` wrong state |
| `POST /api/shop/quotes` (shop) | `200` total, currency, breakdown | `400` invalid request |
| `POST /api/payments` (payment) | `200` status `APPROVED` or `DECLINED` | `400` invalid request |

Every route needs HTTP Basic credentials; without them the answer is `401`.

## Authentication and secrets
- Each service protects its whole API with HTTP Basic auth and one API user.
- Passwords are Docker secrets: Compose mounts them at `/run/secrets/<name>`, and Spring reads
  that folder as configuration (`spring.config.import: configtree:/run/secrets/`).
- Each container only gets the secrets it needs. The hotel management service also gets the
  shop and payment passwords, because it calls those services.
- A service refuses to start if its password is missing.
- To rotate a password: delete its file in `secrets/`, run `./scripts/generate-secrets.sh`,
  then `docker compose up -d --force-recreate`.

## Run without Docker

```bash
mvn package
```

Start each jar with its password in the environment, for example:

```bash
shop_api_password=$(cat secrets/shop_api_password.txt) java -jar shop-service/target/shop-service.jar
```

The hotel management service needs `hotel_api_password`, `shop_api_password` and
`payment_api_password`, and finds the other services on `localhost:8081` and `localhost:8082`.

## Tests

```bash
mvn verify
```

The same command runs on every push and pull request to `main`
(`.github/workflows/tests.yml`). Each service has:

| Test | Covers |
|---|---|
| Use-case tests (`QuoteStayInteractorTest`, `ProcessPaymentInteractorTest`, `ReservationUseCasesTest`) | Business rules, with a recording presenter and stub gateways |
| API tests (`ShopApiTest`, `PaymentApiTest`, `ReservationApiTest`) | Routes, HTTP status codes and authentication, through the full Spring context |
| `ServiceClientsTest` (hotel management) | The exact HTTP calls made to the shop and payment services |
| `ArchitectureTest` | Layer rules, no imports between services, no Spring in the core layers |

## Project layout

| Module | Business purpose | Entry point |
|---|---|---|
| `hotel-management-service` | Rooms, guests, reservations and their lifecycle, guest notifications | `FrontDeskController` |
| `shop-service` | Pricing strategies and additional services | `ShopController` |
| `payment-service` | Charging the guest through an external payment provider | `PaymentController` |

Each module is organised with clean architecture (`controller`, `presenter`, `model`, `usecase`,
`entity`, `gateway`, `infrastructure`, `config`). See [docs/architecture.md](docs/architecture.md)
for the diagrams and rules.

## What is intentionally simplified
- Data is stored in memory and resets when a service restarts; run one instance per service.
- The payment gateway is simulated; no real payment is taken.
- One room (204) is seeded at startup.
- Room availability only looks at the room's current status, not at booked dates.
- Auth is HTTP Basic over plain HTTP with one user per service; put HTTPS in front for anything real.

## Pattern map
| Pattern | Service | Main implementation |
|---|---|---|
| Strategy | shop | `PricingStrategy`, `StandardPricing`, `SeasonalPricing`, `LongStayDiscountPricing` |
| Decorator | shop | `Chargeable`, `RoomStayCharge`, `ServiceDecorator`, service decorators |
| State | hotel management | `ReservationState` and concrete reservation states |
| Observer | hotel management | `ReservationObserver`, `ReservationEventPublisher`, notifier classes |
| Adapter | payment | `PaymentProcessor`, `PayFlowGatewayAdapter`, `PayFlowGateway` |
| Facade | hotel management | `FrontDeskController` |

## Try changing the demo
- In `shop-service/.../config/ShopConfig.java`, replace `SeasonalPricing` with `StandardPricing` or `LongStayDiscountPricing`.
- Toggle `breakfast`, `spa` or `airportTransfer` in the booking request to add/remove `BreakfastService`, `SpaPackage`, or `AirportTransfer`.
- Call the payment service with `"amount": 0` to see the simulated gateway decline.
- Book with a declined payment, then try to check in to see the State pattern reject it (`409`).
# asd-hotel-reservation-nonaislop
