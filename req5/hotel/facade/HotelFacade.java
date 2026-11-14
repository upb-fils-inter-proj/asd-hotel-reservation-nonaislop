package hotel.facade;

import java.time.LocalDate;
import hotel.charges.*;
import hotel.model.*;
import hotel.notification.ReservationEventPublisher;
import hotel.payment.PaymentService;
import hotel.pricing.PricingPolicy;
import hotel.services.ReservationService;
import hotel.services.RoomAvailabilityService;
import hotel.services.RoomService;

public class HotelFacade {
    private final RoomAvailabilityService availabilityService;
    private final PricingPolicy pricingPolicy;
    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final RoomService roomService;
    private final ReservationEventPublisher eventPublisher;

    public HotelFacade(RoomAvailabilityService availabilityService,
                       PricingPolicy pricingPolicy,
                       ReservationService reservationService,
                       PaymentService paymentService,
                       RoomService roomService,
                       ReservationEventPublisher eventPublisher) {
        this.availabilityService = availabilityService;
        this.pricingPolicy = pricingPolicy;
        this.reservationService = reservationService;
        this.paymentService = paymentService;
        this.roomService = roomService;
        this.eventPublisher = eventPublisher;
    }

    public Reservation bookRoom(Guest guest, Room room, LocalDate checkIn,
                                LocalDate checkOut, boolean breakfast,
                                boolean spa, boolean airportTransfer) {
        System.out.println("\n=== BOOKING THROUGH HOTEL FACADE ===");

        if (!availabilityService.isAvailable(room)) {
            System.out.println("Booking failed: room is not available.");
            return null;
        }

        int nights = (int) (checkOut.toEpochDay() - checkIn.toEpochDay());
        if (nights <= 0) {
            System.out.println("Booking failed: check-out must be after check-in.");
            return null;
        }

        double roomPrice = pricingPolicy.calculate(room.getNightlyRate(), nights);
        Chargeable charge = new RoomStayCharge(roomPrice);

        if (breakfast) charge = new BreakfastService(charge, 15.0 * nights);
        if (spa) charge = new SpaPackage(charge, 45.0);
        if (airportTransfer) charge = new AirportTransfer(charge, 30.0);

        System.out.println("Charge breakdown: " + charge.getDescription());
        System.out.printf("Total: EUR %.2f%n", charge.getCost());

        Reservation reservation = reservationService.createReservation(
                guest, room, checkIn, checkOut, charge.getCost());

        boolean paid = paymentService.pay(reservation.getId(), reservation.getTotalPrice());
        if (!paid) {
            System.out.println("Payment failed. Reservation remains pending.");
            return reservation;
        }

        reservation.confirm();
        eventPublisher.notifyObservers(reservation, "CONFIRMED");
        return reservation;
    }

    public void checkIn(Reservation reservation) {
        System.out.println("\n=== CHECK-IN THROUGH HOTEL FACADE ===");
        if (reservation == null) return;

        String previousState = reservation.getState().getName();
        reservation.checkIn();
        if (!previousState.equals(reservation.getState().getName())) {
            roomService.markOccupied(reservation.getRoom());
            eventPublisher.notifyObservers(reservation, "CHECKED_IN");
        }
    }

    public void checkOut(Reservation reservation) {
        System.out.println("\n=== CHECK-OUT THROUGH HOTEL FACADE ===");
        if (reservation == null) return;

        String previousState = reservation.getState().getName();
        reservation.checkOut();
        if (!previousState.equals(reservation.getState().getName())) {
            roomService.markAvailable(reservation.getRoom());
            eventPublisher.notifyObservers(reservation, "CHECKED_OUT");
        }
    }
}
