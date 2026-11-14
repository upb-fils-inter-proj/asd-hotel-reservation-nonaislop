package org.nightking.hotelmanagement.usecase;

import org.nightking.hotelmanagement.model.RegisterGuestRequest;

/** Creates a guest account; a guest must have one before booking. */
public interface RegisterGuestUseCase {
    void execute(RegisterGuestRequest request, AccountPresenter presenter);
}
