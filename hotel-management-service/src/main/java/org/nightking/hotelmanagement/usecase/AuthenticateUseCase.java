package org.nightking.hotelmanagement.usecase;

import org.nightking.hotelmanagement.model.AuthenticateRequest;

/** Checks a username and password and returns the account (guest or admin). */
public interface AuthenticateUseCase {
    void execute(AuthenticateRequest request, AccountPresenter presenter);
}
