package org.nightking.hotelmanagement.gateway;

/** One-way password hashing, provided by infrastructure. */
public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String hash);
}
