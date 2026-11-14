# Architecture: three services, clean architecture inside each

The proof of concept is split into three services by business purpose. Every service is laid
out the same way, and a service talks to another one only through that service's controller.

| Service | Business purpose | Entry point |
|---|---|---|
| `hotelmanagement` | Rooms, guests, reservations and their lifecycle, guest notifications | `FrontDeskController` |
| `shop` | Pricing strategies and additional services (breakfast, spa, airport transfer) | `ShopController` |
| `payment` | Charging the guest through an external payment provider | `PaymentController` |

Layers inside each service:

| Layer | Holds | May depend on |
|---|---|---|
| `controller` | The service's entry point | `usecase`, `model` |
| `model` | Request/response records that cross the controller | `entity` (only to build responses) |
| `usecase` | One interface per use case, plus its implementation (interactor) | `entity`, `model`, `gateway` |
| `entity` | Core business objects and rules | nothing outside `entity` |
| `gateway` | Interfaces for what the use cases need from outside | `entity` |
| `infrastructure` | Implementations of the gateways: storage, external SDKs, clients of other services | `gateway`, `entity`, `model`, and **another service's `controller`/`model` only** |

The shop needs nothing from outside, so it has no `gateway` or `infrastructure` layer.

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

    subgraph hm["hotelmanagement"]
        direction TB
        hm_c["controller<br/>FrontDeskController"]
        hm_u["usecase<br/>BookRoom · CheckIn · CheckOut<br/>ReservationEventPublisher"]
        hm_e["entity<br/>Guest · Room · Reservation<br/>lifecycle states"]
        hm_g["gateway<br/>ReservationObserver"]
        hm_i["infrastructure<br/>Email · SMS · Audit notifiers"]
    end

    subgraph sh["shop"]
        direction TB
        sh_u["usecase<br/>QuoteStay"]
        sh_e["entity<br/>pricing strategies<br/>charge decorators"]
    end

    subgraph pa["payment"]
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

Solid arrows point in the direction of a dependency (always inward, toward the entities).
Dashed arrows mean "implements". The thick arrows are the only calls between services.

```mermaid
flowchart LR
    client(["Main / any client"])

    subgraph HM["Hotel management service"]
        direction TB
        hmC["<b>controller</b><br/>FrontDeskController"]
        hmM["<b>model</b><br/>BookRoomRequest · GuestDetails<br/>ReservationResponse"]
        hmU["<b>usecase</b><br/>BookRoom · CheckIn · CheckOut<br/>ReservationEventPublisher"]
        hmE["<b>entity</b><br/>Reservation · Room · Guest<br/>lifecycle states"]
        hmG["<b>gateway</b> (interfaces)<br/>RoomRepository · ReservationRepository<br/>ShopGateway · PaymentGateway<br/>ReservationObserver"]
        hmI["<b>infrastructure</b><br/>in-memory repositories<br/>Email · SMS · Audit notifiers<br/>ShopServiceClient · PaymentServiceClient"]
        hmC --> hmU
        hmC --> hmM
        hmU --> hmM
        hmU --> hmE
        hmU --> hmG
        hmI -. implements .-> hmG
    end

    subgraph SH["Shop service"]
        direction TB
        shC["<b>controller</b><br/>ShopController"]
        shM["<b>model</b><br/>StayQuoteRequest · StayQuoteResponse"]
        shU["<b>usecase</b><br/>QuoteStay"]
        shE["<b>entity</b><br/>PricingPolicy + strategies<br/>Chargeable + decorators"]
        shC --> shU
        shC --> shM
        shU --> shM
        shU --> shE
    end

    subgraph PA["Payment service"]
        direction TB
        paC["<b>controller</b><br/>PaymentController"]
        paM["<b>model</b><br/>PaymentRequest · PaymentResponse"]
        paU["<b>usecase</b><br/>ProcessPayment"]
        paE["<b>entity</b><br/>Payment · PaymentStatus"]
        paG["<b>gateway</b> (interface)<br/>PaymentProcessor"]
        paI["<b>infrastructure</b><br/>PayFlowGatewayAdapter → PayFlowGateway"]
        paC --> paU
        paC --> paM
        paU --> paM
        paU --> paE
        paU --> paG
        paI -. implements .-> paG
    end

    client --> hmC
    hmI == "quote a stay" ==> shC
    hmI == "charge the guest" ==> paC
```

## 3. A booking across the three services

```mermaid
sequenceDiagram
    autonumber
    actor C as Main / client
    box Hotel management
        participant FD as FrontDeskController
        participant BR as BookRoomInteractor
        participant R as Repositories
    end
    box Shop
        participant SC as ShopController
        participant QS as QuoteStayInteractor
    end
    box Payment
        participant PC as PaymentController
        participant PP as ProcessPaymentInteractor
        participant PF as PayFlowGatewayAdapter
    end

    C->>FD: bookRoom(BookRoomRequest)
    FD->>BR: execute(request)
    BR->>R: findByNumber("204")
    BR->>SC: quoteStay(StayQuoteRequest) via ShopServiceClient
    SC->>QS: execute(request)
    QS-->>SC: StayQuoteResponse
    SC-->>BR: total = EUR 450.00
    BR->>R: nextId(), save(reservation PENDING)
    BR->>PC: processPayment(PaymentRequest) via PaymentServiceClient
    PC->>PP: execute(request)
    PP->>PF: processPayment(id, 450.00)
    PF-->>PP: approved
    PP-->>PC: PaymentResponse(approved)
    PC-->>BR: approved
    BR->>BR: reservation.confirm()
    BR->>R: save(reservation CONFIRMED)
    Note over BR: notify Email, SMS and Audit observers
    BR-->>FD: ReservationResponse(CONFIRMED)
    FD-->>C: ReservationResponse(CONFIRMED)
```

## Rules that keep the services independent

1. **Dependencies point inward.** `controller → usecase → entity`. Use cases reach the outside
   world only through `gateway` interfaces, and `infrastructure` implements them.
2. **Entities never leave a service.** Controllers take and return `model` records.
3. **One door between services.** A service calls another only from its `infrastructure`
   layer, and only through that service's `controller` and `model`
   (`ShopServiceClient`, `PaymentServiceClient`).
4. **`Main` is the composition root.** It is the only class that knows every concrete class
   and wires the three services together.

Today the services run in one JVM and call each other in-process. To deploy them separately,
each service package becomes its own module with its own `Main`, and the two client classes
switch from calling the controller directly to calling it over HTTP. Rule 3 is what keeps
that change limited to those two classes.

## Where every original class went

| Before | After |
|---|---|
| `frontdesk.FrontDesk` | Split: `hotelmanagement.controller.FrontDeskController` + `BookRoom`, `CheckIn`, `CheckOut` use cases; the pricing and extras part moved to `shop.usecase.QuoteStayInteractor` |
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

New in the split: the `model` records, the use-case interfaces, the repository and service
gateways with their implementations, the three controllers, and the `Payment` entity.
