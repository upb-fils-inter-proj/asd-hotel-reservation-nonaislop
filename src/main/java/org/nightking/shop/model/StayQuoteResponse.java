package org.nightking.shop.model;

/** Price of a stay, with a readable breakdown of what was charged. */
public record StayQuoteResponse(double total, String breakdown) {
}
