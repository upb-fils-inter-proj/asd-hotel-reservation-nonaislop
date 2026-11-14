package org.nightking.payment.config;

import java.time.Clock;
import org.nightking.payment.gateway.PaymentProcessor;
import org.nightking.payment.gateway.PaymentRepository;
import org.nightking.payment.infrastructure.PayFlowGateway;
import org.nightking.payment.infrastructure.PayFlowGatewayAdapter;
import org.nightking.payment.infrastructure.persistence.JpaPaymentRepository;
import org.nightking.payment.infrastructure.persistence.PaymentJpaRepository;
import org.nightking.payment.usecase.ProcessPaymentInteractor;
import org.nightking.payment.usecase.ProcessPaymentUseCase;
import org.nightking.payment.usecase.RefundPaymentInteractor;
import org.nightking.payment.usecase.RefundPaymentUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root of the payment service. */
@Configuration
public class PaymentConfig {

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
}
