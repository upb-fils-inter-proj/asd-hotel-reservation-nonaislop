# Hotel Management and Reservation System — Java Proof of Concept

A small Java proof of concept demonstrating six design patterns from the UML design:
Strategy, Decorator, State, Observer, Adapter, and Facade.

## Requirements
- JDK 21 (see `maven.compiler.source` in `pom.xml`)
- Maven (or any IDE that can build a Maven project)
- No external libraries or database required

## Build and run

Compile and run the demo with Maven:

```bash
mvn compile
java -cp target/classes org.nightking.Main
```

Or run `org.nightking.Main` directly from your IDE.

## Tests

```bash
mvn test
```

JUnit 5 tests live under `src/test/java`, one class per service plus two cross-cutting ones:

| Test | Covers |
|---|---|
| `shop.ShopControllerTest` | Pricing strategies and additional services |
| `payment.PaymentControllerTest` | Approval, decline, and what is passed to the payment provider |
| `hotelmanagement.FrontDeskControllerTest` | Booking, check-in and check-out paths, with the shop and payment services stubbed out |
| `ServicesIntegrationTest` | All three services wired as in `Main` |
| `ArchitectureTest` | The layer and cross-service rules from [docs/architecture.md](docs/architecture.md) |

The program runs a sample booking, adds optional services, calculates a price,
processes a simulated payment, confirms the reservation, and demonstrates
check-in and check-out. It also prints notifications and audit events.

## Project layout

The code under `src/main/java/org/nightking` is split into three services, each organised
with clean architecture (`controller`, `model`, `usecase`, `entity`, `gateway`, `infrastructure`):

| Service | Business purpose | Entry point |
|---|---|---|
| `hotelmanagement` | Rooms, guests, reservations and their lifecycle, guest notifications | `FrontDeskController` |
| `shop` | Pricing strategies and additional services | `ShopController` |
| `payment` | Charging the guest through an external payment provider | `PaymentController` |

`Main` starts all three in one JVM and wires them together. Services only call each other
through controllers. See [docs/architecture.md](docs/architecture.md) for the diagrams, the
layer rules and where every original class went.

## What is intentionally simplified
- Data is stored in memory; there is no database or web interface.
- The payment gateway is simulated; no real payment is taken.
- The sample uses fixed rooms, guests, prices, and dates.
- The focus is on pattern collaboration, not production features.

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
- In `Main.java`, replace `SeasonalPricing` with `StandardPricing` or `LongStayDiscountPricing`.
- Toggle breakfast, spa or airport transfer in the `BookRoomRequest` in `Main.java` to add/remove `BreakfastService`, `SpaPackage`, or `AirportTransfer`.
- Change the payment amount or the simulated gateway result.
- Try calling `checkIn()` before payment confirmation to see the State pattern reject it.
# asd-hotel-reservation-nonaislop
