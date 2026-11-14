package org.nightking.shop.model;

/** Everything the shop needs to price a stay. */
public record StayQuoteRequest(double nightlyRate,
                               int nights,
                               boolean breakfast,
                               boolean spa,
                               boolean airportTransfer) {
}
