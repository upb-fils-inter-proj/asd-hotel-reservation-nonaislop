package org.nightking.payment.controller;

import org.nightking.payment.model.PaymentRequest;
import org.nightking.payment.presenter.PaymentWebPresenter;
import org.nightking.payment.usecase.ProcessPaymentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Entry point of the payment service. */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final ProcessPaymentUseCase processPayment;

    public PaymentController(ProcessPaymentUseCase processPayment) {
        this.processPayment = processPayment;
    }

    @PostMapping
    public ResponseEntity<Object> processPayment(@RequestBody PaymentRequest request) {
        PaymentWebPresenter presenter = new PaymentWebPresenter();
        processPayment.execute(request, presenter);
        return presenter.response();
    }
}
