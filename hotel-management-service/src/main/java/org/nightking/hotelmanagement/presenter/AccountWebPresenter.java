package org.nightking.hotelmanagement.presenter;

import org.nightking.hotelmanagement.model.AccountResponse;
import org.nightking.hotelmanagement.usecase.AccountPresenter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AccountWebPresenter implements AccountPresenter {
    private ResponseEntity<Object> response;

    @Override
    public void presentRegistered(AccountResponse account) {
        response = ResponseEntity.status(HttpStatus.CREATED).body(AccountViewModel.from(account));
    }

    @Override
    public void presentAuthenticated(AccountResponse account) {
        response = ResponseEntity.ok(AccountViewModel.from(account));
    }

    @Override
    public void presentInvalidCredentials(String message) {
        response = error(HttpStatus.UNAUTHORIZED, message);
    }

    @Override
    public void presentRejected(String message) {
        response = error(HttpStatus.CONFLICT, message);
    }

    @Override
    public void presentInvalidRequest(String message) {
        response = error(HttpStatus.BAD_REQUEST, message);
    }

    public ResponseEntity<Object> response() {
        if (response == null) {
            throw new IllegalStateException("The use case did not present a result.");
        }
        return response;
    }

    private static ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorViewModel(message));
    }
}
