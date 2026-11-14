package org.nightking.webapp.model;

/** The signed-in user, as returned by the hotel management service. */
public record Account(String id, String username, String role, String name, String email, String phone) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
