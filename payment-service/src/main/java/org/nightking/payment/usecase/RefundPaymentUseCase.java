package org.nightking.payment.usecase;

/** Gives an approved payment back to the guest (used when a reservation is cancelled). */
public interface RefundPaymentUseCase {
    void execute(String transactionId, RefundPresenter presenter);
}
