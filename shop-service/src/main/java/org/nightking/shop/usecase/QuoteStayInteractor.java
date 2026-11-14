package org.nightking.shop.usecase;

import org.nightking.shop.entity.Extra;
import org.nightking.shop.entity.charges.AirportTransfer;
import org.nightking.shop.entity.charges.BreakfastService;
import org.nightking.shop.entity.charges.Chargeable;
import org.nightking.shop.entity.charges.RoomStayCharge;
import org.nightking.shop.entity.charges.SpaPackage;
import org.nightking.shop.entity.pricing.PricingPolicy;
import org.nightking.shop.gateway.ExtrasCatalog;
import org.nightking.shop.model.StayQuoteRequest;
import org.nightking.shop.model.StayQuoteResponse;

public class QuoteStayInteractor implements QuoteStayUseCase {
    private final PricingPolicy pricingPolicy;
    private final ExtrasCatalog extras;

    public QuoteStayInteractor(PricingPolicy pricingPolicy, ExtrasCatalog extras) {
        this.pricingPolicy = pricingPolicy;
        this.extras = extras;
    }

    @Override
    public void execute(StayQuoteRequest request, QuoteStayPresenter presenter) {
        if (request.nights() <= 0 || request.nightlyRate() <= 0) {
            presenter.presentInvalidRequest("nights and nightlyRate must be greater than zero.");
            return;
        }

        double roomPrice = pricingPolicy.calculate(request.nightlyRate(), request.nights());
        Chargeable charge = new RoomStayCharge(roomPrice);

        if (request.breakfast()) charge = new BreakfastService(charge, extras.priceOf(Extra.BREAKFAST) * request.nights());
        if (request.spa()) charge = new SpaPackage(charge, extras.priceOf(Extra.SPA));
        if (request.airportTransfer()) charge = new AirportTransfer(charge, extras.priceOf(Extra.AIRPORT_TRANSFER));

        System.out.println("Charge breakdown: " + charge.getDescription());
        System.out.printf("Total: EUR %.2f%n", charge.getCost());

        presenter.presentQuote(new StayQuoteResponse(charge.getCost(), charge.getDescription()));
    }
}
