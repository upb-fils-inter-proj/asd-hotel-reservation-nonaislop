package hotel;

import java.time.LocalDate;
import hotel.facade.HotelFacade;
import hotel.model.Guest;
import hotel.model.Reservation;
import hotel.model.Room;
import hotel.notification.*;
import hotel.payment.*;
import hotel.pricing.*;
import hotel.services.*;

public class Main {
    public static void main(String[] args) {
  
        ReservationEventPublisher publisher = new ReservationEventPublisher();
        publisher.addObserver(new EmailNotifier());
        publisher.addObserver(new SmsNotifier());
        publisher.addObserver(new AuditLogObserver());

        PricingPolicy pricingPolicy = new PricingPolicy(new SeasonalPricing(1.20));

        PaymentProcessor adapter = new PayFlowGatewayAdapter(new PayFlowGateway());
        PaymentService paymentService = new PaymentService(adapter);

        HotelFacade hotel = new HotelFacade(
                new RoomAvailabilityService(),
                pricingPolicy,
                new ReservationService(),
                paymentService,
                new RoomService(),
                publisher
        );

        Guest guest = new Guest("G-01", "Alex Morgan",
                "alex@gmail.com", "+0769123420");
        Room room = new Room("204", "Deluxe", 100.00);

        Reservation reservation = hotel.bookRoom(
                guest,
                room,
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 13),
                true,
                true,
                false
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
