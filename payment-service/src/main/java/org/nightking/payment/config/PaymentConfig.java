package org.nightking.payment.config;

import org.nightking.payment.gateway.PaymentProcessor;
import org.nightking.payment.gateway.PaymentRepository;
import org.nightking.payment.infrastructure.PayFlowGateway;
import org.nightking.payment.infrastructure.PayFlowGatewayAdapter;
import org.nightking.payment.infrastructure.persistence.JpaPaymentRepository;
import org.nightking.payment.infrastructure.persistence.PaymentJpaRepository;
import org.nightking.payment.usecase.ProcessPaymentInteractor;
import org.nightking.payment.usecase.ProcessPaymentUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root of the payment service. */
@Configuration
public class PaymentConfig {

    @Bean
    PaymentProcessor paymentProcessor() {
        // Connect the incompatible gateway through the Adapter.
        return new PayFlowGatewayAdapter(new PayFlowGateway());
    }

    @Bean
    PaymentRepository paymentRepository(PaymentJpaRepository payments) {
        return new JpaPaymentRepository(payments);
    }

    @Bean
    ProcessPaymentUseCase processPaymentUseCase(PaymentProcessor paymentProcessor, PaymentRepository payments) {
        return new ProcessPaymentInteractor(paymentProcessor, payments);
    }
}
