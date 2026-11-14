package org.nightking.shop.usecase;

import org.nightking.shop.entity.charges.AirportTransfer;
import org.nightking.shop.entity.charges.BreakfastService;
import org.nightking.shop.entity.charges.Chargeable;
import org.nightking.shop.entity.charges.RoomStayCharge;
import org.nightking.shop.entity.charges.SpaPackage;
import org.nightking.shop.entity.pricing.PricingPolicy;
import org.nightking.shop.model.StayQuoteRequest;
import org.nightking.shop.model.StayQuoteResponse;

public class QuoteStayInteractor implements QuoteStayUseCase {
    private static final double BREAKFAST_PRICE_PER_NIGHT = 15.0;
    private static final double SPA_PACKAGE_PRICE = 45.0;
    private static final double AIRPORT_TRANSFER_PRICE = 30.0;

    private final PricingPolicy pricingPolicy;

    public QuoteStayInteractor(PricingPolicy pricingPolicy) {
        this.pricingPolicy = pricingPolicy;
    }

    @Override
    public void execute(StayQuoteRequest request, QuoteStayPresenter presenter) {
        if (request.nights() <= 0 || request.nightlyRate() <= 0) {
            presenter.presentInvalidRequest("nights and nightlyRate must be greater than zero.");
            return;
        }

        double roomPrice = pricingPolicy.calculate(request.nightlyRate(), request.nights());
        Chargeable charge = new RoomStayCharge(roomPrice);

        if (request.breakfast()) charge = new BreakfastService(charge, BREAKFAST_PRICE_PER_NIGHT * request.nights());
        if (request.spa()) charge = new SpaPackage(charge, SPA_PACKAGE_PRICE);
        if (request.airportTransfer()) charge = new AirportTransfer(charge, AIRPORT_TRANSFER_PRICE);

        System.out.println("Charge breakdown: " + charge.getDescription());
        System.out.printf("Total: EUR %.2f%n", charge.getCost());

        presenter.presentQuote(new StayQuoteResponse(charge.getCost(), charge.getDescription()));
    }
}
