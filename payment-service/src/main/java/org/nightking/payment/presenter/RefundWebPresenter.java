package org.nightking.payment.presenter;

import org.nightking.payment.model.PaymentResponse;
import org.nightking.payment.usecase.RefundPresenter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RefundWebPresenter implements RefundPresenter {
    private ResponseEntity<Object> response;

    @Override
    public void presentRefunded(PaymentResponse payment) {
        response = ResponseEntity.ok(PaymentViewModel.from(payment));
    }

    @Override
    public void presentNotFound(String message) {
        response = ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorViewModel(message));
    }

    @Override
    public void presentRejected(String message) {
        response = ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorViewModel(message));
    }

    public ResponseEntity<Object> response() {
        if (response == null) {
            throw new IllegalStateException("The use case did not present a result.");
        }
        return response;
    }
}
