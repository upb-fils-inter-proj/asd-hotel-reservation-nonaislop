package org.nightking.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nightking.payment.model.PaymentRequest;
import org.nightking.payment.presenter.MessageReply;
import org.nightking.payment.presenter.PaymentMessagePresenter;
import org.nightking.payment.usecase.ProcessPaymentUseCase;
import org.nightking.payment.usecase.RefundPaymentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT entry point of the payment service (topics payment/charges/request and payment/refunds/request):
 * the message counterpart of {@link PaymentController}. Takes the request JSON, runs the use case, returns the reply JSON.
 */
public class PaymentMessageController {
    private static final Logger log = LoggerFactory.getLogger(PaymentMessageController.class);

    private final ProcessPaymentUseCase processPayment;
    private final RefundPaymentUseCase refundPayment;
    private final ObjectMapper json;

    public PaymentMessageController(ProcessPaymentUseCase processPayment, RefundPaymentUseCase refundPayment,
                                    ObjectMapper json) {
        this.processPayment = processPayment;
        this.refundPayment = refundPayment;
        this.json = json;
    }

    public String charge(String requestJson) {
        return handle("charge", () -> {
            PaymentRequest request = json.readValue(requestJson, PaymentRequest.class);
            PaymentMessagePresenter presenter = new PaymentMessagePresenter();
            processPayment.execute(request, presenter);
            return presenter.reply();
        });
    }

    public String refund(String requestJson) {
        return handle("refund", () -> {
            RefundRequest request = json.readValue(requestJson, RefundRequest.class);
            if (request.transactionId() == null || request.transactionId().isBlank()) {
                return MessageReply.invalidRequest("transactionId is required.");
            }
            PaymentMessagePresenter presenter = new PaymentMessagePresenter();
            refundPayment.execute(request.transactionId(), presenter);
            return presenter.reply();
        });
    }

    /** The refund message only names the payment to give back. */
    record RefundRequest(String transactionId) {}

    private interface Action {
        MessageReply run() throws JsonProcessingException;
    }

    private String handle(String what, Action action) {
        MessageReply reply;
        try {
            reply = action.run();
        } catch (JsonProcessingException e) {
            reply = MessageReply.invalidRequest("Unreadable " + what + " request: " + e.getOriginalMessage());
        } catch (RuntimeException e) {
            log.error("{} request failed", what, e);
            reply = MessageReply.error("The payment service could not process the " + what + ".");
        }
        try {
            return json.writeValueAsString(reply);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise the reply", e);
        }
    }
}
