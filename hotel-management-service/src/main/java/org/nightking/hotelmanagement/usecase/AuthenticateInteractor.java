package org.nightking.hotelmanagement.usecase;

import java.util.Optional;
import org.nightking.hotelmanagement.entity.Account;
import org.nightking.hotelmanagement.gateway.AccountRepository;
import org.nightking.hotelmanagement.gateway.PasswordHasher;
import org.nightking.hotelmanagement.model.AccountResponse;
import org.nightking.hotelmanagement.model.AuthenticateRequest;

public class AuthenticateInteractor implements AuthenticateUseCase {
    private static final String INVALID = "Invalid username or password.";

    private final AccountRepository accounts;
    private final PasswordHasher passwordHasher;

    public AuthenticateInteractor(AccountRepository accounts, PasswordHasher passwordHasher) {
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void execute(AuthenticateRequest request, AccountPresenter presenter) {
        if (request.username() == null || request.password() == null) {
            presenter.presentInvalidCredentials(INVALID);
            return;
        }
        Optional<Account> account = accounts.findByUsername(request.username());
        if (account.isEmpty() || !passwordHasher.matches(request.password(), account.get().getPasswordHash())) {
            presenter.presentInvalidCredentials(INVALID);
            return;
        }
        presenter.presentAuthenticated(AccountResponse.from(account.get()));
    }
}
