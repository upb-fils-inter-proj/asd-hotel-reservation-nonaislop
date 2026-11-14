# Architecture: a web app in front of three Spring Boot services

The system is split by business purpose. A web app serves guests and administrators; behind it,
three services each own their business rules and their own PostgreSQL database. Each service is
its own Maven module, Spring Boot application and Docker container. Services talk only over
HTTP, through REST controllers, with HTTP Basic auth.

| Module | Business purpose | Entry point | Database |
|---|---|---|---|
| `web-app` | Guest and administration web interface | Thymeleaf pages on port 8090 | none |
| `hotel-management-service` | Rooms, guest and admin accounts, reservations and their lifecycle, notifications | `/api/reservations`, `/api/rooms`, `/api/accounts` | `hotel` |
| `shop-service` | Pricing strategies and the additional services price list | `/api/shop/quotes` | `shop` |
| `payment-service` | Charging the guest and recording every payment | `/api/payments` | `payment` |

Layers inside each service:

| Layer | Holds | May depend on |
|---|---|---|
| `controller` | REST endpoints; creates a presenter per request and calls a use case | `usecase`, `model`, `presenter` |
| `presenter` | Implements the use case's output port; builds the JSON view model and HTTP status | `usecase`, `model` |
| `model` | Request/response records crossing the use case boundary | `entity` (only to build responses) |
| `usecase` | One interface per use case, its implementation (interactor), and its presenter interface (output port) | `entity`, `model`, `gateway` |
| `entity` | Core business objects and rules | nothing outside `entity` |
| `gateway` | Interfaces for what the use cases need from outside: repositories, other services, password hashing | `entity` |
| `infrastructure` | Gateway implementations: the database (JPA), HTTP clients, external SDKs, notifiers | `gateway`, `entity`, `model` |
| `config` | Spring wiring (composition root) and security | everything in the service |

`entity`, `model`, `gateway` and `usecase` contain no Spring, JPA or Jackson code. The web app
has no business rules, so it has no `usecase` layer: its controllers call a
`HotelManagementGateway` directly. Every rule here is enforced by the `ArchitectureTest` in each
module.

## 1. How the original code was split

Each package of the original single application went to one service; the old `FrontDesk`
was split between hotel management (orchestration) and the shop (pricing and extras).

```mermaid
flowchart LR
    subgraph before["Before: one application"]
        direction TB
        b_fd["frontdesk<br/>FrontDesk"]
        b_dom["guest · room · reservation"]
        b_lc["reservation.lifecycle"]
        b_not["notification"]
        b_pri["pricing · charges"]
        b_pay["payment"]
    end

    subgraph hm["hotel-management-service"]
        direction TB
        hm_c["controller<br/>FrontDeskController"]
        hm_u["usecase<br/>BookRoom · CheckIn · CheckOut<br/>ReservationEventPublisher"]
        hm_e["entity<br/>Guest · Room · Reservation<br/>lifecycle states"]
        hm_g["gateway<br/>ReservationObserver"]
        hm_i["infrastructure<br/>Email · SMS · Audit notifiers"]
    end

    subgraph sh["shop-service"]
        direction TB
        sh_u["usecase<br/>QuoteStay"]
        sh_e["entity<br/>pricing strategies<br/>charge decorators"]
    end

    subgraph pa["payment-service"]
        direction TB
        pa_u["usecase<br/>ProcessPayment"]
        pa_g["gateway<br/>PaymentProcessor"]
        pa_i["infrastructure<br/>PayFlowGateway + adapter"]
    end

    b_fd -- "entry point" --> hm_c
    b_fd -- "booking flow" --> hm_u
    b_fd -- "pricing + extras" --> sh_u
    b_dom --> hm_e
    b_lc --> hm_e
    b_not --> hm_g
    b_not --> hm_u
    b_not --> hm_i
    b_pri --> sh_e
    b_pay -- "PaymentService" --> pa_u
    b_pay --> pa_g
    b_pay --> pa_i
```

## 2. Inside the services, and the database as a plugin

Solid arrows point in the direction of a dependency (inward, toward the entities). Dashed arrows
mean "implements". Each database sits behind a gateway interface: the use cases only know
`RoomRepository`, `ExtrasCatalog`, `PaymentRepository` and so on, and a JPA adapter in
`infrastructure` plugs PostgreSQL in. The thick arrows are the only calls between processes.

```mermaid
flowchart LR
    browser(["Browser<br/>guest · admin"])

    subgraph WA["web-app :8090"]
        direction TB
        waC["<b>controller</b><br/>pages · login · registration<br/>guest + admin pages"]
        waG["<b>gateway</b><br/>HotelManagementGateway"]
        waI["<b>infrastructure</b><br/>HotelManagementClient"]
        waC --> waG
        waI -. implements .-> waG
    end

    subgraph HM["hotel-management-service :8080"]
        direction TB
        hmC["<b>controller + presenter</b><br/>FrontDeskController · RoomController<br/>AccountController"]
        hmU["<b>usecase</b><br/>BookRoom · CheckIn · CheckOut · ListReservations<br/>ListRooms · RegisterGuest · Authenticate · SeedAdmin"]
        hmE["<b>entity</b><br/>Reservation + lifecycle · Room · Account"]
        hmG["<b>gateway</b><br/>RoomRepository · ReservationRepository<br/>AccountRepository · PasswordHasher<br/>ShopGateway · PaymentGateway"]
        hmI["<b>infrastructure</b><br/>JPA repositories · BCrypt<br/>ShopServiceClient · PaymentServiceClient"]
        hmC --> hmU
        hmU --> hmE
        hmU --> hmG
        hmI -. implements .-> hmG
    end

    subgraph SH["shop-service :8081"]
        direction TB
        shC["<b>controller + presenter</b><br/>ShopController"]
        shU["<b>usecase</b><br/>QuoteStay"]
        shE["<b>entity</b><br/>pricing strategies · decorators · Extra"]
        shG["<b>gateway</b><br/>ExtrasCatalog"]
        shI["<b>infrastructure</b><br/>JpaExtrasCatalog"]
        shC --> shU
        shU --> shE
        shU --> shG
        shI -. implements .-> shG
    end

    subgraph PA["payment-service :8082"]
        direction TB
        paC["<b>controller + presenter</b><br/>PaymentController"]
        paU["<b>usecase</b><br/>ProcessPayment"]
        paE["<b>entity</b><br/>Payment"]
        paG["<b>gateway</b><br/>PaymentProcessor · PaymentRepository"]
        paI["<b>infrastructure</b><br/>PayFlow adapter · JpaPaymentRepository"]
        paC --> paU
        paU --> paE
        paU --> paG
        paI -. implements .-> paG
    end

    hdb[("hotel DB")]
    sdb[("shop DB")]
    pdb[("payment DB")]

    browser -- "form login<br/>session + CSRF" --> waC
    waI == "REST + Basic auth<br/>front-desk" ==> hmC
    hmI == "POST /api/shop/quotes" ==> shC
    hmI == "POST /api/payments" ==> paC
    hmI --> hdb
    shI --> sdb
    paI --> pdb
```

## 3. A guest books a room

```mermaid
sequenceDiagram
    autonumber
    actor G as Guest (browser)
    participant W as web-app
    box Hotel management
        participant BR as BookRoomInteractor
        participant HDB as hotel DB
    end
    participant S as shop-service
    participant P as payment-service

    G->>W: POST /login (username, password)
    W->>BR: POST /api/accounts/authenticate
    Note over W,BR: AuthenticateInteractor checks the bcrypt hash in the hotel DB
    BR-->>W: 200 account (role GUEST)
    W-->>G: session cookie, redirect to My reservations
    G->>W: search dates, choose room, pick extras
    W->>BR: GET /api/rooms/available, POST /api/reservations/quote
    BR->>S: POST /api/shop/quotes
    S-->>W: total + breakdown (prices from the shop DB)
    G->>W: POST /book/204/confirm (card) + CSRF token
    W->>BR: POST /api/reservations (guestId from the session, card)
    BR->>HDB: guest exists? dates valid? room free for those nights?
    BR->>HDB: save PENDING + hold room_nights (unique per room and night)
    BR->>P: POST /api/payments (card)
    Note over P: validate card, charge via PayFlow adapter,<br/>store brand + last 4 only
    alt card approved
        P-->>BR: 200 {status: APPROVED, transactionId}
        BR->>HDB: save CONFIRMED + payment reference
        Note over BR: notify Email / SMS / Audit observers
        BR-->>W: 201 reservation
        W-->>G: "Payment approved (Visa •••• 4242). Reservation confirmed."
    else card declined
        P-->>BR: 200 {status: DECLINED, declineReason}
        BR->>HDB: save CANCELLED, release room_nights
        BR-->>W: 402 "Payment declined: Insufficient funds."
        W-->>G: back to review with the reason
    end
```

## 4. Deployment with Docker Compose

Only the web app is meant for people; the service APIs are published for development. Databases
are not published at all. Each container gets only the secrets it needs.

```mermaid
flowchart LR
    browser(["Browser"])
    subgraph compose["docker compose: hotel-reservation"]
        wa["web-app<br/>:8090"]
        hm["hotel-management-service<br/>:8080"]
        sh["shop-service<br/>:8081"]
        pa["payment-service<br/>:8082"]
        hdb[("hotel-db")]
        sdb[("shop-db")]
        pdb[("payment-db")]
    end
    browser --> wa
    wa --> hm
    hm --> sh
    hm --> pa
    hm --> hdb
    sh --> sdb
    pa --> pdb
```

| Container | Secrets it receives |
|---|---|
| `web-app` | `hotel_api_password` (to call hotel management) |
| `hotel-management-service` | `hotel_api_password`, `hotel_db_password`, `admin_password`, `shop_api_password`, `payment_api_password` |
| `shop-service` | `shop_api_password`, `shop_db_password` |
| `payment-service` | `payment_api_password`, `payment_db_password` |
| `hotel-db` / `shop-db` / `payment-db` | its own `*_db_password` |

## Databases and consistency

Each service owns one database and no other service reads it:

| Database | Tables | Owner |
|---|---|---|
| `hotel` | `rooms`, `accounts`, `reservations`, `room_nights` | hotel management |
| `shop` | `extra_prices` | shop |
| `payment` | `payments` (brand and last four digits only, never full card numbers) | payment |

Schemas are created by Flyway migrations in each service (`src/main/resources/db/migration`).

No data is duplicated across databases, so there is nothing to keep in sync. Two flows touch two
databases, and both are ordered so that a failure at any step leaves each database valid:

- **Booking:** the reservation is saved as `PENDING` and holds its nights in the hotel database;
  then the payment service charges the card and records the payment. Approved → the reservation
  becomes `CONFIRMED` and keeps the payment reference. Declined (or the payment service is
  unreachable) → the reservation becomes `CANCELLED` and releases its nights.
- **Cancellation:** the refund is made first; the reservation is saved as `CANCELLED` only after
  the payment service confirms it.

**No double bookings.** `room_nights` (room, night, reservation) has a primary key on
(room, night). The JPA adapter writes those rows in the same transaction as the reservation while
its state holds the room (pending, confirmed, checked in) and deletes them otherwise. A second
reservation for any of the same nights violates the key, the adapter reports
`RoomUnavailableException`, and the use case answers "already booked" — even when two bookings
pass the availability check at the same moment.

If more data ever has to be shared, the services would exchange events and accept eventual
consistency rather than share a database.

## Rules that keep the services independent

1. **Dependencies point inward.** `controller → usecase → entity`. Use cases reach the outside
   world, including their database, only through `gateway` interfaces, and send results out only
   through their presenter interface.
2. **The database is a plugin.** JPA entities and Spring Data repositories live in
   `infrastructure`; domain entities know nothing about persistence. The use-case tests run the
   same rules on in-memory gateways.
3. **One database per service, no shared code.** Each service is a separate module with its own
   schema; the hotel management clients keep their own copy of the shop and payment API shapes.
4. **Every call is authenticated.** People log in to the web app; services call each other with
   HTTP Basic credentials. All passwords are Docker secrets, never committed.
5. **`config` is the composition root.** It is the only place that knows every concrete class.

## Where every original class went

| Before | After |
|---|---|
| `frontdesk.FrontDesk` | Split: `FrontDeskController` + `BookRoom`, `CheckIn`, `CheckOut` use cases in hotel management; the pricing and extras part moved to the shop's `QuoteStayInteractor` |
| `guest.Guest`, `room.Room`, `room.RoomStatus`, `reservation.Reservation` | `hotelmanagement.entity` |
| `reservation.lifecycle.*` | `hotelmanagement.entity.lifecycle` |
| `room.RoomAvailabilityService` | Date-based availability: `Reservation.blocks(from, to)` with `ReservationState.holdsRoom()` |
| `room.RoomService` | Room status updates inside `CheckInInteractor` / `CheckOutInteractor` |
| `reservation.ReservationService` | Reservation numbers from a database sequence (`ReservationRepository.nextId()`) |
| `notification.ReservationObserver` | `hotelmanagement.gateway` |
| `notification.ReservationEventPublisher` | `hotelmanagement.usecase` |
| `notification.EmailNotifier`, `SmsNotifier`, `AuditLogObserver` | `hotelmanagement.infrastructure` |
| `pricing.*` | `shop.entity.pricing` |
| `charges.*` | `shop.entity.charges` (prices now come from the shop database) |
| `payment.PaymentProcessor` | `payment.gateway` |
| `payment.PayFlowGateway`, `PayFlowGatewayAdapter` | `payment.infrastructure` |
| `payment.PaymentService` | `payment.usecase.ProcessPaymentInteractor` |
