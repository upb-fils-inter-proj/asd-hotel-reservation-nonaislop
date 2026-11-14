package org.nightking.hotelmanagement.config;

import org.nightking.hotelmanagement.gateway.AccountRepository;
import org.nightking.hotelmanagement.gateway.PasswordHasher;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.nightking.hotelmanagement.infrastructure.AuditLogObserver;
import org.nightking.hotelmanagement.infrastructure.BCryptPasswordHasher;
import org.nightking.hotelmanagement.infrastructure.EmailNotifier;
import org.nightking.hotelmanagement.infrastructure.PaymentServiceClient;
import org.nightking.hotelmanagement.infrastructure.ShopServiceClient;
import org.nightking.hotelmanagement.infrastructure.SmsNotifier;
import org.nightking.hotelmanagement.infrastructure.persistence.AccountJpaRepository;
import org.nightking.hotelmanagement.infrastructure.persistence.JpaAccountRepository;
import org.nightking.hotelmanagement.infrastructure.persistence.JpaReservationRepository;
import org.nightking.hotelmanagement.infrastructure.persistence.JpaRoomRepository;
import org.nightking.hotelmanagement.infrastructure.persistence.ReservationJpaRepository;
import org.nightking.hotelmanagement.infrastructure.persistence.RoomJpaRepository;
import org.nightking.hotelmanagement.usecase.AuthenticateInteractor;
import org.nightking.hotelmanagement.usecase.AuthenticateUseCase;
import org.nightking.hotelmanagement.usecase.BookRoomInteractor;
import org.nightking.hotelmanagement.usecase.BookRoomUseCase;
import org.nightking.hotelmanagement.usecase.CheckInInteractor;
import org.nightking.hotelmanagement.usecase.CheckInUseCase;
import org.nightking.hotelmanagement.usecase.CheckOutInteractor;
import org.nightking.hotelmanagement.usecase.CheckOutUseCase;
import org.nightking.hotelmanagement.usecase.ListReservationsInteractor;
import org.nightking.hotelmanagement.usecase.ListReservationsUseCase;
import org.nightking.hotelmanagement.usecase.ListRoomsInteractor;
import org.nightking.hotelmanagement.usecase.ListRoomsUseCase;
import org.nightking.hotelmanagement.usecase.RegisterGuestInteractor;
import org.nightking.hotelmanagement.usecase.RegisterGuestUseCase;
import org.nightking.hotelmanagement.usecase.ReservationEventPublisher;
import org.nightking.hotelmanagement.usecase.SeedAdminAccountInteractor;
import org.nightking.hotelmanagement.usecase.SeedAdminAccountUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Composition root of the hotel management service: plugs the database and HTTP adapters into the use cases. */
@Configuration
public class HotelManagementConfig {
    private static final Logger log = LoggerFactory.getLogger(HotelManagementConfig.class);

    // --- gateways: the hotel database (PostgreSQL in Docker, H2 in tests)

    @Bean
    RoomRepository roomRepository(RoomJpaRepository rooms) {
        return new JpaRoomRepository(rooms);
    }

    @Bean
    AccountRepository accountRepository(AccountJpaRepository accounts) {
        return new JpaAccountRepository(accounts);
    }

    @Bean
    ReservationRepository reservationRepository(ReservationJpaRepository reservations,
                                                RoomJpaRepository rooms,
                                                AccountJpaRepository accounts) {
        return new JpaReservationRepository(reservations, rooms, accounts);
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    // --- gateways: the other services, over HTTP

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
    ReservationEventPublisher reservationEventPublisher() {
        // Set up Observer subscribers.
        ReservationEventPublisher publisher = new ReservationEventPublisher();
        publisher.addObserver(new EmailNotifier());
        publisher.addObserver(new SmsNotifier());
        publisher.addObserver(new AuditLogObserver());
        return publisher;
    }

    // --- use cases

    @Bean
    BookRoomUseCase bookRoomUseCase(AccountRepository accounts, RoomRepository rooms,
                                    ReservationRepository reservations, ShopGateway shop,
                                    PaymentGateway payment, ReservationEventPublisher publisher) {
        return new BookRoomInteractor(accounts, rooms, reservations, shop, payment, publisher);
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

    @Bean
    ListReservationsUseCase listReservationsUseCase(ReservationRepository reservations) {
        return new ListReservationsInteractor(reservations);
    }

    @Bean
    ListRoomsUseCase listRoomsUseCase(RoomRepository rooms) {
        return new ListRoomsInteractor(rooms);
    }

    @Bean
    RegisterGuestUseCase registerGuestUseCase(AccountRepository accounts, PasswordHasher passwordHasher) {
        return new RegisterGuestInteractor(accounts, passwordHasher);
    }

    @Bean
    AuthenticateUseCase authenticateUseCase(AccountRepository accounts, PasswordHasher passwordHasher) {
        return new AuthenticateInteractor(accounts, passwordHasher);
    }

    @Bean
    SeedAdminAccountUseCase seedAdminAccountUseCase(AccountRepository accounts, PasswordHasher passwordHasher) {
        return new SeedAdminAccountInteractor(accounts, passwordHasher);
    }

    /** Pre-creates the administrator from the admin_password secret when the service starts. */
    @Bean
    ApplicationRunner seedAdminAccount(SeedAdminAccountUseCase seedAdmin,
                                       @Value("${admin.username}") String username,
                                       @Value("${admin.password}") String password) {
        return args -> {
            if (password == null || password.isBlank()) {
                log.warn("admin.password is empty (admin_password secret missing): no administrator account created.");
                return;
            }
            seedAdmin.execute(username, password);
        };
    }
}
