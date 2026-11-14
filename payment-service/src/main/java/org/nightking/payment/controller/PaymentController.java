package org.nightking.payment.controller;

import org.nightking.payment.model.PaymentRequest;
import org.nightking.payment.presenter.PaymentWebPresenter;
import org.nightking.payment.presenter.RefundWebPresenter;
import org.nightking.payment.usecase.ProcessPaymentUseCase;
import org.nightking.payment.usecase.RefundPaymentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Entry point of the payment service: charge a card and refund a payment. */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final ProcessPaymentUseCase processPayment;
    private final RefundPaymentUseCase refundPayment;

    public PaymentController(ProcessPaymentUseCase processPayment, RefundPaymentUseCase refundPayment) {
        this.processPayment = processPayment;
        this.refundPayment = refundPayment;
    }

    @PostMapping
    public ResponseEntity<Object> processPayment(@RequestBody PaymentRequest request) {
        PaymentWebPresenter presenter = new PaymentWebPresenter();
        processPayment.execute(request, presenter);
        return presenter.response();
    }

    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<Object> refund(@PathVariable String transactionId) {
        RefundWebPresenter presenter = new RefundWebPresenter();
        refundPayment.execute(transactionId, presenter);
        return presenter.response();
    }
}
