# Architecture: three Spring Boot services, clean architecture inside each

The system is split into three services by business purpose. Each service is its own Maven
module, Spring Boot application and Docker container. A service talks to another one only over
HTTP, through that service's REST controller, with HTTP Basic auth.

| Service (module) | Business purpose | Entry point | Port |
|---|---|---|---|
| `hotel-management-service` | Rooms, guests, reservations and their lifecycle, guest notifications | `FrontDeskController` → `/api/reservations` | 8080 |
| `shop-service` | Pricing strategies and additional services (breakfast, spa, airport transfer) | `ShopController` → `/api/shop/quotes` | 8081 |
| `payment-service` | Charging the guest through an external payment provider | `PaymentController` → `/api/payments` | 8082 |

Layers inside each service:

| Layer | Holds | May depend on |
|---|---|---|
| `controller` | REST endpoints; creates a presenter per request and calls a use case | `usecase`, `model`, `presenter` |
| `presenter` | Implements the use case's output port; builds the JSON view model and HTTP status | `usecase`, `model` |
| `model` | Request/response records crossing the use case boundary | `entity` (only to build responses) |
| `usecase` | One interface per use case, its implementation (interactor), and its presenter interface (output port) | `entity`, `model`, `gateway` |
| `entity` | Core business objects and rules | nothing outside `entity` |
| `gateway` | Interfaces for what the use cases need from outside | `entity` |
| `infrastructure` | Gateway implementations: storage, external SDKs, HTTP clients of other services | `gateway`, `entity`, `model` |
| `config` | Spring wiring (composition root) and security | everything in the service |

`entity`, `model`, `gateway` and `usecase` contain no Spring code. The shop needs nothing from
outside, so it has no `gateway` or `infrastructure` layer. Every rule here is enforced by the
`ArchitectureTest` in each module.

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

## 2. Inside the services and how they talk

Solid arrows point in the direction of a dependency (inward, toward the entities). Dashed arrows
mean "implements". The thick arrows are the only calls between services: HTTP with Basic auth.

```mermaid
flowchart LR
    client(["API client<br/>curl · front-end"])

    subgraph HM["hotel-management-service :8080"]
        direction TB
        hmC["<b>controller</b><br/>FrontDeskController<br/>/api/reservations"]
        hmP["<b>presenter</b><br/>ReservationWebPresenter<br/>ReservationViewModel"]
        hmU["<b>usecase</b><br/>BookRoom · CheckIn · CheckOut<br/>ReservationPresenter (output port)"]
        hmM["<b>model</b><br/>BookRoomRequest · ReservationResponse"]
        hmE["<b>entity</b><br/>Reservation · Room · Guest<br/>lifecycle states"]
        hmG["<b>gateway</b><br/>RoomRepository · ReservationRepository<br/>ShopGateway · PaymentGateway<br/>ReservationObserver"]
        hmI["<b>infrastructure</b><br/>in-memory repositories · notifiers<br/>ShopServiceClient · PaymentServiceClient"]
        hmC --> hmU
        hmC --> hmP
        hmP -. implements .-> hmU
        hmU --> hmM
        hmU --> hmE
        hmU --> hmG
        hmI -. implements .-> hmG
    end

    subgraph SH["shop-service :8081"]
        direction TB
        shC["<b>controller</b><br/>ShopController<br/>/api/shop/quotes"]
        shP["<b>presenter</b><br/>QuoteStayWebPresenter"]
        shU["<b>usecase</b><br/>QuoteStay"]
        shE["<b>entity</b><br/>PricingPolicy + strategies<br/>Chargeable + decorators"]
        shC --> shU
        shC --> shP
        shP -. implements .-> shU
        shU --> shE
    end

    subgraph PA["payment-service :8082"]
        direction TB
        paC["<b>controller</b><br/>PaymentController<br/>/api/payments"]
        paP["<b>presenter</b><br/>PaymentWebPresenter"]
        paU["<b>usecase</b><br/>ProcessPayment"]
        paE["<b>entity</b><br/>Payment · PaymentStatus"]
        paG["<b>gateway</b><br/>PaymentProcessor"]
        paI["<b>infrastructure</b><br/>PayFlowGatewayAdapter → PayFlowGateway"]
        paC --> paU
        paC --> paP
        paP -. implements .-> paU
        paU --> paE
        paU --> paG
        paI -. implements .-> paG
    end

    client -- "Basic auth<br/>front-desk" --> hmC
    hmI == "POST /api/shop/quotes<br/>Basic auth shop-client" ==> shC
    hmI == "POST /api/payments<br/>Basic auth payment-client" ==> paC
```

## 3. A booking across the three services

```mermaid
sequenceDiagram
    autonumber
    actor C as API client
    box Hotel management
        participant FD as FrontDeskController
        participant BR as BookRoomInteractor
        participant P as ReservationWebPresenter
    end
    box Shop
        participant SC as ShopController
    end
    box Payment
        participant PC as PaymentController
        participant PF as PayFlowGatewayAdapter
    end

    C->>FD: POST /api/reservations (Basic auth)
    FD->>BR: execute(request, presenter)
    BR->>SC: POST /api/shop/quotes via ShopServiceClient
    SC-->>BR: 200 {total: 450.00}
    BR->>BR: save reservation (PENDING)
    BR->>PC: POST /api/payments via PaymentServiceClient
    PC->>PF: processPayment(id, 450.00)
    PF-->>PC: approved
    PC-->>BR: 200 {status: APPROVED}
    BR->>BR: confirm(), notify Email / SMS / Audit
    BR->>P: presentBooked(ReservationResponse)
    P-->>FD: 201 + ReservationViewModel
    FD-->>C: 201 {state: CONFIRMED, totalPrice: 450.00}
```

## 4. Deployment with Docker Compose

Each service runs in its own container. Each container gets only the secrets it needs; the
hotel management service also holds the passwords of the services it calls.

```mermaid
flowchart LR
    client(["API client"])
    subgraph compose["docker compose: hotel-reservation"]
        hm["hotel-management-service<br/>:8080"]
        sh["shop-service<br/>:8081"]
        pa["payment-service<br/>:8082"]
    end
    s1[/"hotel_api_password"/]
    s2[/"shop_api_password"/]
    s3[/"payment_api_password"/]

    client --> hm
    hm --> sh
    hm --> pa
    s1 -. secret .-> hm
    s2 -. secret .-> hm
    s3 -. secret .-> hm
    s2 -. secret .-> sh
    s3 -. secret .-> pa
```

## Rules that keep the services independent

1. **Dependencies point inward.** `controller → usecase → entity`. Use cases reach the outside
   world only through `gateway` interfaces and send results out only through their presenter
   interface.
2. **Entities never leave a service.** Use cases hand `model` records to presenters, and
   presenters turn them into view models for JSON.
3. **No shared code between services.** Each service is a separate module. The hotel management
   clients keep their own copy of the shop and payment request/response shapes.
4. **Every call is authenticated.** Each API requires HTTP Basic auth; passwords are Docker
   secrets, never committed.
5. **`config` is the composition root.** It is the only place that knows every concrete class.

## Where every original class went

| Before | After |
|---|---|
| `frontdesk.FrontDesk` | Split: `FrontDeskController` + `BookRoom`, `CheckIn`, `CheckOut` use cases in hotel management; the pricing and extras part moved to the shop's `QuoteStayInteractor` |
| `guest.Guest`, `room.Room`, `room.RoomStatus`, `reservation.Reservation` | `hotelmanagement.entity` |
| `reservation.lifecycle.*` | `hotelmanagement.entity.lifecycle` |
| `room.RoomAvailabilityService` | `Room.isAvailable()` |
| `room.RoomService` | Room status updates inside `CheckInInteractor` / `CheckOutInteractor` |
| `reservation.ReservationService` | Id sequence in `InMemoryReservationRepository.nextId()` |
| `notification.ReservationObserver` | `hotelmanagement.gateway` |
| `notification.ReservationEventPublisher` | `hotelmanagement.usecase` |
| `notification.EmailNotifier`, `SmsNotifier`, `AuditLogObserver` | `hotelmanagement.infrastructure` |
| `pricing.*` | `shop.entity.pricing` |
| `charges.*` | `shop.entity.charges` |
| `payment.PaymentProcessor` | `payment.gateway` |
| `payment.PayFlowGateway`, `PayFlowGatewayAdapter` | `payment.infrastructure` |
| `payment.PaymentService` | `payment.usecase.ProcessPaymentInteractor` |
