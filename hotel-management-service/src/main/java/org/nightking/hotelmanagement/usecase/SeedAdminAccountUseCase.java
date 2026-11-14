package org.nightking.hotelmanagement.usecase;

/**
 * Makes sure the pre-created administrator exists with the configured password.
 * Runs at startup; administrators are never registered through the API.
 */
public interface SeedAdminAccountUseCase {
    void execute(String username, String password);
}
