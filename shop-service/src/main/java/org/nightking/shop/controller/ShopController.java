package org.nightking.shop.controller;

import org.nightking.shop.model.StayQuoteRequest;
import org.nightking.shop.presenter.QuoteStayWebPresenter;
import org.nightking.shop.usecase.QuoteStayUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Entry point of the shop service: pricing and additional services. */
@RestController
@RequestMapping("/api/shop")
public class ShopController {
    private final QuoteStayUseCase quoteStay;

    public ShopController(QuoteStayUseCase quoteStay) {
        this.quoteStay = quoteStay;
    }

    @PostMapping("/quotes")
    public ResponseEntity<Object> quoteStay(@RequestBody StayQuoteRequest request) {
        QuoteStayWebPresenter presenter = new QuoteStayWebPresenter();
        quoteStay.execute(request, presenter);
        return presenter.response();
    }
}
