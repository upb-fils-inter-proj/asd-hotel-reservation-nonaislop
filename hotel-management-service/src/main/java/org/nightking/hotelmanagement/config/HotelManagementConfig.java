package org.nightking.hotelmanagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.nightking.hotelmanagement.gateway.AccountRepository;
import org.nightking.hotelmanagement.gateway.PasswordHasher;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.nightking.hotelmanagement.infrastructure.AuditLogObserver;
import org.nightking.hotelmanagement.infrastructure.BCryptPasswordHasher;
import org.nightking.hotelmanagement.infrastructure.EmailNotifier;
import org.nightking.hotelmanagement.infrastructure.SmsNotifier;
import org.nightking.hotelmanagement.infrastructure.messaging.MqttRequester;
import org.nightking.hotelmanagement.infrastructure.messaging.MqttReservationEventPublisher;
import org.nightking.hotelmanagement.infrastructure.messaging.MqttTopics;
import org.nightking.hotelmanagement.infrastructure.messaging.MqttTransport;
import org.nightking.hotelmanagement.infrastructure.messaging.PahoMqttTransport;
import org.nightking.hotelmanagement.infrastructure.messaging.PaymentMqttClient;
import org.nightking.hotelmanagement.infrastructure.messaging.ShopMqttClient;
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
import org.nightking.hotelmanagement.usecase.CancelReservationInteractor;
import org.nightking.hotelmanagement.usecase.CancelReservationUseCase;
import org.nightking.hotelmanagement.usecase.QuoteBookingInteractor;
import org.nightking.hotelmanagement.usecase.QuoteBookingUseCase;
import org.nightking.hotelmanagement.usecase.SearchAvailableRoomsInteractor;
import org.nightking.hotelmanagement.usecase.SearchAvailableRoomsUseCase;
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
import org.springframework.transaction.PlatformTransactionManager;

/** Composition root of the hotel management service: plugs the database and MQTT adapters into the use cases. */
@Configuration
public class HotelManagementConfig {
    private static final Logger log = LoggerFactory.getLogger(HotelManagementConfig.class);

    /** Names this running instance on the broker; its replies arrive on hotel-management/replies/{instanceId}. */
    private final String instanceId = "hotel-management-" + UUID.randomUUID().toString().substring(0, 8);

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
                                                AccountJpaRepository accounts,
                                                PlatformTransactionManager transactionManager) {
        return new JpaReservationRepository(reservations, rooms, accounts, transactionManager);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    // --- gateways: the other services, over MQTT

    @Bean
    MqttTransport mqttTransport(@Value("${messaging.mqtt.url}") String url,
                                @Value("${messaging.mqtt.username}") String username,
                                @Value("${messaging.mqtt.password}") String password) {
        PahoMqttTransport transport = new PahoMqttTransport(url, instanceId, username, password);
        transport.start();
        return transport;
    }

    @Bean
    MqttRequester mqttRequester(MqttTransport transport, ObjectMapper json,
                                @Value("${messaging.mqtt.request-timeout}") Duration timeout) {
        return new MqttRequester(transport, json, MqttTopics.REPLIES_PREFIX + instanceId, timeout);
    }

    @Bean
    ShopGateway shopGateway(MqttRequester requester) {
        return new ShopMqttClient(requester);
    }

    @Bean
    PaymentGateway paymentGateway(MqttRequester requester) {
        return new PaymentMqttClient(requester);
    }

    @Bean
    ReservationEventPublisher reservationEventPublisher(MqttTransport transport, ObjectMapper json, Clock clock) {
        // Set up Observer subscribers.
        ReservationEventPublisher publisher = new ReservationEventPublisher();
        publisher.addObserver(new EmailNotifier());
        publisher.addObserver(new SmsNotifier());
        publisher.addObserver(new AuditLogObserver());
        publisher.addObserver(new MqttReservationEventPublisher(transport, json, clock));
        return publisher;
    }

    // --- use cases

    @Bean
    BookRoomUseCase bookRoomUseCase(AccountRepository accounts, RoomRepository rooms,
                                    ReservationRepository reservations, ShopGateway shop,
                                    PaymentGateway payment, ReservationEventPublisher publisher, Clock clock) {
        return new BookRoomInteractor(accounts, rooms, reservations, shop, payment, publisher, clock);
    }

    @Bean
    SearchAvailableRoomsUseCase searchAvailableRoomsUseCase(RoomRepository rooms, ReservationRepository reservations,
                                                            Clock clock) {
        return new SearchAvailableRoomsInteractor(rooms, reservations, clock);
    }

    @Bean
    QuoteBookingUseCase quoteBookingUseCase(RoomRepository rooms, ReservationRepository reservations,
                                            ShopGateway shop, Clock clock) {
        return new QuoteBookingInteractor(rooms, reservations, shop, clock);
    }

    @Bean
    CancelReservationUseCase cancelReservationUseCase(ReservationRepository reservations, PaymentGateway payment,
                                                      ReservationEventPublisher publisher) {
        return new CancelReservationInteractor(reservations, payment, publisher);
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
