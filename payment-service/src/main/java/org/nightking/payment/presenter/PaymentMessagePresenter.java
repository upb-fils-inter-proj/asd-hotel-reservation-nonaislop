package org.nightking.payment.presenter;

import org.nightking.payment.model.PaymentResponse;
import org.nightking.payment.usecase.ProcessPaymentPresenter;
import org.nightking.payment.usecase.RefundPresenter;

/** Turns the charge and refund use case results into an MQTT reply. One instance per request. */
public class PaymentMessagePresenter implements ProcessPaymentPresenter, RefundPresenter {
    private MessageReply reply;

    @Override
    public void presentResult(PaymentResponse payment) {
        reply = MessageReply.ok(PaymentViewModel.from(payment));
    }

    @Override
    public void presentRefunded(PaymentResponse payment) {
        reply = MessageReply.ok(PaymentViewModel.from(payment));
    }

    @Override
    public void presentInvalidRequest(String reason) {
        reply = MessageReply.invalidRequest(reason);
    }

    @Override
    public void presentNotFound(String message) {
        reply = MessageReply.notFound(message);
    }

    @Override
    public void presentRejected(String message) {
        reply = MessageReply.rejected(message);
    }

    public MessageReply reply() {
        if (reply == null) {
            throw new IllegalStateException("The use case did not present a result.");
        }
        return reply;
    }
}
