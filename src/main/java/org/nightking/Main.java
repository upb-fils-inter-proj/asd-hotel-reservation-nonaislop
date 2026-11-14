package org.nightking;

import java.time.LocalDate;
import org.nightking.facade.HotelFacade;
import org.nightking.model.Guest;
import org.nightking.model.Reservation;
import org.nightking.model.Room;
import org.nightking.notification.*;
import org.nightking.payment.*;
import org.nightking.pricing.*;
import org.nightking.services.*;

public class Main {
    public static void main(String[] args) {
        // Set up Observer subscribers.
        ReservationEventPublisher publisher = new ReservationEventPublisher();
        publisher.addObserver(new EmailNotifier());
        publisher.addObserver(new SmsNotifier());
        publisher.addObserver(new AuditLogObserver());

        // Select the pricing Strategy for this demonstration.
        PricingPolicy pricingPolicy = new PricingPolicy(new SeasonalPricing(1.20));
        // Alternative strategies:
        // new StandardPricing()
        // new LongStayDiscountPricing(5, 0.10)

        // Connect the incompatible gateway through the Adapter.
        PaymentProcessor adapter = new PayFlowGatewayAdapter(new PayFlowGateway());
        PaymentService paymentService = new PaymentService(adapter);

        // Assemble the Facade and its collaborating services.
        HotelFacade hotel = new HotelFacade(
                new RoomAvailabilityService(),
                pricingPolicy,
                new ReservationService(),
                paymentService,
                new RoomService(),
                publisher
        );

        Guest guest = new Guest("G-01", "Alex Morgan",
                "alex@example.com", "+40123456789");
        Room room = new Room("204", "Deluxe", 100.00);

        Reservation reservation = hotel.bookRoom(
                guest,
                room,
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 13),
                true,   // breakfast decorator
                true,   // spa decorator
                false   // airport transfer decorator
        );

        if (reservation != null) {
            System.out.println("\nCurrent reservation state: "
                    + reservation.getState().getName());
            hotel.checkIn(reservation);
            System.out.println("State after check-in: "
                    + reservation.getState().getName());
            hotel.checkOut(reservation);
            System.out.println("Final reservation state: "
                    + reservation.getState().getName());
        }
    }
}
