package org.nightking.hotelmanagement.model;

/** guestId is set when a guest cancels their own reservation; empty when an administrator cancels. */
public record CancelRequest(String guestId) {
}
