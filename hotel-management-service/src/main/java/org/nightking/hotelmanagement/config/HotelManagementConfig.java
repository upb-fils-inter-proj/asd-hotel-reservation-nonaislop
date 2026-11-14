package org.nightking.hotelmanagement.config;

import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.nightking.hotelmanagement.infrastructure.AuditLogObserver;
import org.nightking.hotelmanagement.infrastructure.EmailNotifier;
import org.nightking.hotelmanagement.infrastructure.InMemoryReservationRepository;
import org.nightking.hotelmanagement.infrastructure.InMemoryRoomRepository;
import org.nightking.hotelmanagement.infrastructure.PaymentServiceClient;
import org.nightking.hotelmanagement.infrastructure.ShopServiceClient;
import org.nightking.hotelmanagement.infrastructure.SmsNotifier;
import org.nightking.hotelmanagement.usecase.BookRoomInteractor;
import org.nightking.hotelmanagement.usecase.BookRoomUseCase;
import org.nightking.hotelmanagement.usecase.CheckInInteractor;
import org.nightking.hotelmanagement.usecase.CheckInUseCase;
import org.nightking.hotelmanagement.usecase.CheckOutInteractor;
import org.nightking.hotelmanagement.usecase.CheckOutUseCase;
import org.nightking.hotelmanagement.usecase.ReservationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Composition root of the hotel management service. */
@Configuration
public class HotelManagementConfig {

    @Bean
    RoomRepository roomRepository() {
        RoomRepository rooms = new InMemoryRoomRepository();
        rooms.save(new Room("204", "Deluxe", 100.00));
        return rooms;
    }

    @Bean
    ReservationRepository reservationRepository() {
        return new InMemoryReservationRepository();
    }

    @Bean
    ReservationEventPublisher reservationEventPublisher() {
        // Set up Observer subscribers.
        ReservationEventPublisher publisher = new ReservationEventPublisher();
        publisher.addObserver(new EmailNotifier());
        publisher.addObserver(new SmsNotifier());
        publisher.addObserver(new AuditLogObserver());
        return publisher;
    }

    @Bean
    ShopGateway shopGateway(RestClient.Builder builder,
                            @Value("${services.shop.url}") String url,
                            @Value("${services.shop.username}") String username,
                            @Value("${services.shop.password}") String password) {
        return new ShopServiceClient(builder.clone(), url, username, password);
    }

    @Bean
    PaymentGateway paymentGateway(RestClient.Builder builder,
                                  @Value("${services.payment.url}") String url,
                                  @Value("${services.payment.username}") String username,
                                  @Value("${services.payment.password}") String password) {
        return new PaymentServiceClient(builder.clone(), url, username, password);
    }

    @Bean
    BookRoomUseCase bookRoomUseCase(RoomRepository rooms, ReservationRepository reservations,
                                    ShopGateway shop, PaymentGateway payment,
                                    ReservationEventPublisher publisher) {
        return new BookRoomInteractor(rooms, reservations, shop, payment, publisher);
    }

    @Bean
    CheckInUseCase checkInUseCase(RoomRepository rooms, ReservationRepository reservations,
                                  ReservationEventPublisher publisher) {
        return new CheckInInteractor(rooms, reservations, publisher);
    }

    @Bean
    CheckOutUseCase checkOutUseCase(RoomRepository rooms, ReservationRepository reservations,
                                    ReservationEventPublisher publisher) {
        return new CheckOutInteractor(rooms, reservations, publisher);
    }
}
