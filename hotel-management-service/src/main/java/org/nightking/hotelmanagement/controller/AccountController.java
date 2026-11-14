package org.nightking.hotelmanagement.controller;

import org.nightking.hotelmanagement.model.AuthenticateRequest;
import org.nightking.hotelmanagement.model.RegisterGuestRequest;
import org.nightking.hotelmanagement.presenter.AccountWebPresenter;
import org.nightking.hotelmanagement.usecase.AuthenticateUseCase;
import org.nightking.hotelmanagement.usecase.RegisterGuestUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Guest registration and sign-in checks, used by the web app. */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final RegisterGuestUseCase registerGuest;
    private final AuthenticateUseCase authenticate;

    public AccountController(RegisterGuestUseCase registerGuest, AuthenticateUseCase authenticate) {
        this.registerGuest = registerGuest;
        this.authenticate = authenticate;
    }

    @PostMapping
    public ResponseEntity<Object> register(@RequestBody RegisterGuestRequest request) {
        AccountWebPresenter presenter = new AccountWebPresenter();
        registerGuest.execute(request, presenter);
        return presenter.response();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<Object> authenticate(@RequestBody AuthenticateRequest request) {
        AccountWebPresenter presenter = new AccountWebPresenter();
        authenticate.execute(request, presenter);
        return presenter.response();
    }
}
