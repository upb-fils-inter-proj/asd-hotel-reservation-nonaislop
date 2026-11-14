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

The program runs a sample booking, adds optional services, calculates a price,
processes a simulated payment, confirms the reservation, and demonstrates
check-in and check-out. It also prints notifications and audit events.

## Project layout

All code lives under `src/main/java/org/nightking`:

| Package | Contents |
|---|---|
| `org.nightking` | `Main` — demo entry point that wires everything together |
| `model` | `Guest`, `Room`, `RoomStatus`, `Reservation` |
| `pricing` | `PricingStrategy`, `PricingPolicy` and the pricing strategies |
| `charges` | `Chargeable`, `RoomStayCharge`, `ServiceDecorator` and the service decorators |
| `state` | `ReservationState` and the concrete reservation states |
| `notification` | `ReservationObserver`, `ReservationEventPublisher` and the notifiers |
| `payment` | `PaymentProcessor`, `PaymentService`, `PayFlowGateway`, `PayFlowGatewayAdapter` |
| `services` | `ReservationService`, `RoomService`, `RoomAvailabilityService` |
| `facade` | `HotelFacade` — single entry point for booking, check-in and check-out |

## What is intentionally simplified
- Data is stored in memory; there is no database or web interface.
- The payment gateway is simulated; no real payment is taken.
- The sample uses fixed rooms, guests, prices, and dates.
- The focus is on pattern collaboration, not production features.

## Pattern map
| Pattern | Main implementation |
|---|---|
| Strategy | `PricingStrategy`, `StandardPricing`, `SeasonalPricing`, `LongStayDiscountPricing` |
| Decorator | `Chargeable`, `RoomStayCharge`, `ServiceDecorator`, service decorators |
| State | `ReservationState` and concrete reservation states |
| Observer | `ReservationObserver`, `ReservationEventPublisher`, notifier classes |
| Adapter | `PaymentProcessor`, `PayFlowGatewayAdapter`, `PayFlowGateway` |
| Facade | `HotelFacade` |

## Try changing the demo
- In `Main.java`, replace `SeasonalPricing` with `StandardPricing` or `LongStayDiscountPricing`.
- Add/remove `BreakfastService`, `SpaPackage`, or `AirportTransfer`.
- Change the payment amount or the simulated gateway result.
- Try calling `checkIn()` before payment confirmation to see the State pattern reject it.
# asd-hotel-reservation-nonaislop
