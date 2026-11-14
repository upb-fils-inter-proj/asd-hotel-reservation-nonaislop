package org.nightking.hotelmanagement.gateway;

/** What hotel management needs from the shop service: the price of a stay, in EUR. */
public interface ShopGateway {
    StayQuote quoteStay(double nightlyRate, int nights, boolean breakfast, boolean spa, boolean airportTransfer);

    record StayQuote(double total, String breakdown) {}
}
