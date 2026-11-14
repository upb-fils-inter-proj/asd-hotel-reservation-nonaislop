package org.nightking.payment.presenter;

import org.nightking.payment.model.PaymentResponse;
import org.nightking.payment.usecase.ProcessPaymentPresenter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Turns the use case result into an HTTP response. One instance per request. */
public class PaymentWebPresenter implements ProcessPaymentPresenter {
    private ResponseEntity<Object> response;

    @Override
    public void presentResult(PaymentResponse payment) {
        response = ResponseEntity.ok(PaymentViewModel.from(payment));
    }

    @Override
    public void presentInvalidRequest(String reason) {
        response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorViewModel(reason));
    }

    public ResponseEntity<Object> response() {
        if (response == null) {
            throw new IllegalStateException("The use case did not present a result.");
        }
        return response;
    }
}
