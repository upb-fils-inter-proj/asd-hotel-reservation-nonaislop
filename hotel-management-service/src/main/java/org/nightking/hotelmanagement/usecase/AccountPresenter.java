package org.nightking.hotelmanagement.usecase;

import org.nightking.hotelmanagement.model.AccountResponse;

/** Output port of the account use cases. */
public interface AccountPresenter {
    void presentRegistered(AccountResponse account);
    void presentAuthenticated(AccountResponse account);
    void presentInvalidCredentials(String message);
    /** The request was valid but a business rule refused it (username taken). */
    void presentRejected(String message);
    void presentInvalidRequest(String message);
}
