package org.nightking;

import java.time.LocalDate;
import java.util.Optional;
import org.nightking.hotelmanagement.controller.FrontDeskController;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.infrastructure.AuditLogObserver;
import org.nightking.hotelmanagement.infrastructure.EmailNotifier;
import org.nightking.hotelmanagement.infrastructure.InMemoryReservationRepository;
import org.nightking.hotelmanagement.infrastructure.InMemoryRoomRepository;
import org.nightking.hotelmanagement.infrastructure.PaymentServiceClient;
import org.nightking.hotelmanagement.infrastructure.ShopServiceClient;
import org.nightking.hotelmanagement.infrastructure.SmsNotifier;
import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.model.GuestDetails;
import org.nightking.hotelmanagement.model.ReservationResponse;
import org.nightking.hotelmanagement.usecase.BookRoomInteractor;
import org.nightking.hotelmanagement.usecase.CheckInInteractor;
import org.nightking.hotelmanagement.usecase.CheckOutInteractor;
import org.nightking.hotelmanagement.usecase.ReservationEventPublisher;
import org.nightking.payment.controller.PaymentController;
import org.nightking.payment.gateway.PaymentProcessor;
import org.nightking.payment.infrastructure.PayFlowGateway;
import org.nightking.payment.infrastructure.PayFlowGatewayAdapter;
import org.nightking.payment.usecase.ProcessPaymentInteractor;
import org.nightking.shop.controller.ShopController;
import org.nightking.shop.entity.pricing.PricingPolicy;
import org.nightking.shop.entity.pricing.SeasonalPricing;
import org.nightking.shop.usecase.QuoteStayInteractor;

/**
 * Starts the three services in one JVM and runs a sample stay through the front desk.
 * Each service is wired only to the controllers of the services it calls.
 */
public class Main {
    public static void main(String[] args) {
        ShopController shop = createShopService();
        PaymentController payment = createPaymentService();
        FrontDeskController frontDesk = createHotelManagementService(shop, payment);

        GuestDetails guest = new GuestDetails("G-01", "Alex Morgan",
                "alex@example.com", "+40123456789");

        Optional<ReservationResponse> booking = frontDesk.bookRoom(new BookRoomRequest(
                guest,
                "204",
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 13),
                true,   // breakfast decorator
                true,   // spa decorator
                false   // airport transfer decorator
        ));

        booking.ifPresent(reservation -> {
            System.out.println("\nCurrent reservation state: " + reservation.state());
            frontDesk.checkIn(reservation.reservationId())
                    .ifPresent(r -> System.out.println("State after check-in: " + r.state()));
            frontDesk.checkOut(reservation.reservationId())
                    .ifPresent(r -> System.out.println("Final reservation state: " + r.state()));
        });
    }

    static ShopController createShopService() {
        // Select the pricing Strategy for this demonstration.
        PricingPolicy pricingPolicy = new PricingPolicy(new SeasonalPricing(1.20));
        // Alternative strategies:
        // new StandardPricing()
        // new LongStayDiscountPricing(5, 0.10)
        return new ShopController(new QuoteStayInteractor(pricingPolicy));
    }

    static PaymentController createPaymentService() {
        // Connect the incompatible gateway through the Adapter.
        PaymentProcessor adapter = new PayFlowGatewayAdapter(new PayFlowGateway());
        return new PaymentController(new ProcessPaymentInteractor(adapter));
    }

    static FrontDeskController createHotelManagementService(ShopController shop,
                                                            PaymentController payment) {
        // Set up Observer subscribers.
        ReservationEventPublisher publisher = new ReservationEventPublisher();
        publisher.addObserver(new EmailNotifier());
        publisher.addObserver(new SmsNotifier());
        publisher.addObserver(new AuditLogObserver());

        RoomRepository rooms = new InMemoryRoomRepository();
        rooms.save(new Room("204", "Deluxe", 100.00));
        ReservationRepository reservations = new InMemoryReservationRepository();

        // Hotel management only reaches the other services through their controllers.
        ShopServiceClient shopClient = new ShopServiceClient(shop);
        PaymentServiceClient paymentClient = new PaymentServiceClient(payment);

        return new FrontDeskController(
                new BookRoomInteractor(rooms, reservations, shopClient, paymentClient, publisher),
                new CheckInInteractor(rooms, reservations, publisher),
                new CheckOutInteractor(rooms, reservations, publisher));
    }
}
