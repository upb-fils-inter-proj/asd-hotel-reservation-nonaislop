package org.nightking.shop.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nightking.shop.model.StayQuoteRequest;
import org.nightking.shop.presenter.MessageReply;
import org.nightking.shop.presenter.QuoteStayMessagePresenter;
import org.nightking.shop.usecase.QuoteStayUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT entry point of the shop service (topic shop/quotes/request): the message counterpart of
 * {@link ShopController}. Takes the request JSON, runs the use case and returns the reply JSON.
 */
public class ShopMessageController {
    private static final Logger log = LoggerFactory.getLogger(ShopMessageController.class);

    private final QuoteStayUseCase quoteStay;
    private final ObjectMapper json;

    public ShopMessageController(QuoteStayUseCase quoteStay, ObjectMapper json) {
        this.quoteStay = quoteStay;
        this.json = json;
    }

    public String quoteStay(String requestJson) {
        MessageReply reply;
        try {
            StayQuoteRequest request = json.readValue(requestJson, StayQuoteRequest.class);
            QuoteStayMessagePresenter presenter = new QuoteStayMessagePresenter();
            quoteStay.execute(request, presenter);
            reply = presenter.reply();
        } catch (JsonProcessingException e) {
            reply = MessageReply.invalidRequest("Unreadable quote request: " + e.getOriginalMessage());
        } catch (RuntimeException e) {
            log.error("Quote request failed", e);
            reply = MessageReply.error("The shop could not quote the stay.");
        }
        return write(reply);
    }

    private String write(MessageReply reply) {
        try {
            return json.writeValueAsString(reply);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise the reply", e);
        }
    }
}
