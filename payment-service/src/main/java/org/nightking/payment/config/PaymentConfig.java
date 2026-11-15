package org.nightking.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.UUID;
import org.nightking.payment.controller.PaymentMessageController;
import org.nightking.payment.gateway.PaymentProcessor;
import org.nightking.payment.gateway.PaymentRepository;
import org.nightking.payment.infrastructure.PayFlowGateway;
import org.nightking.payment.infrastructure.PayFlowGatewayAdapter;
import org.nightking.payment.infrastructure.messaging.MqttResponder;
import org.nightking.payment.infrastructure.messaging.MqttTransport;
import org.nightking.payment.infrastructure.messaging.PahoMqttTransport;
import org.nightking.payment.infrastructure.persistence.JpaPaymentRepository;
import org.nightking.payment.infrastructure.persistence.PaymentJpaRepository;
import org.nightking.payment.usecase.ProcessPaymentInteractor;
import org.nightking.payment.usecase.ProcessPaymentUseCase;
import org.nightking.payment.usecase.RefundPaymentInteractor;
import org.nightking.payment.usecase.RefundPaymentUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root of the payment service. */
@Configuration
public class PaymentConfig {
    /** Requests are shared between all running payment instances (each request goes to one of them). */
    static final String CHARGE_REQUESTS = "$share/payment-service/payment/charges/request";
    static final String REFUND_REQUESTS = "$share/payment-service/payment/refunds/request";

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    PaymentProcessor paymentProcessor() {
        // Connect the simulated PayFlow card network through the Adapter.
        return new PayFlowGatewayAdapter(new PayFlowGateway());
    }

    @Bean
    PaymentRepository paymentRepository(PaymentJpaRepository payments, Clock clock) {
        return new JpaPaymentRepository(payments, clock);
    }

    @Bean
    ProcessPaymentUseCase processPaymentUseCase(PaymentProcessor paymentProcessor, PaymentRepository payments,
                                                Clock clock) {
        return new ProcessPaymentInteractor(paymentProcessor, payments, clock);
    }

    @Bean
    RefundPaymentUseCase refundPaymentUseCase(PaymentProcessor paymentProcessor, PaymentRepository payments) {
        return new RefundPaymentInteractor(paymentProcessor, payments);
    }

    // --- the MQTT side: the hotel management service asks for charges and refunds through the broker

    @Bean
    MqttTransport mqttTransport(@Value("${messaging.mqtt.url}") String url,
                                @Value("${messaging.mqtt.username}") String username,
                                @Value("${messaging.mqtt.password}") String password) {
        PahoMqttTransport transport = new PahoMqttTransport(url,
                "payment-" + UUID.randomUUID().toString().substring(0, 8), username, password);
        transport.start();
        return transport;
    }

    @Bean
    PaymentMessageController paymentMessageController(ProcessPaymentUseCase processPayment,
                                                      RefundPaymentUseCase refundPayment, ObjectMapper json) {
        return new PaymentMessageController(processPayment, refundPayment, json);
    }

    @Bean
    ApplicationRunner answerPaymentRequests(MqttTransport transport, PaymentMessageController controller) {
        return args -> {
            MqttResponder responder = new MqttResponder(transport);
            responder.serve(CHARGE_REQUESTS, controller::charge);
            responder.serve(REFUND_REQUESTS, controller::refund);
        };
    }
}
