package org.nightking.hotelmanagement.model;

public record RegisterGuestRequest(String username, String password, String name, String email, String phone) {
}
