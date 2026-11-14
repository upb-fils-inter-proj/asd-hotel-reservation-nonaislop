package org.nightking.hotelmanagement.usecase;

import java.util.UUID;
import java.util.regex.Pattern;
import org.nightking.hotelmanagement.entity.Account;
import org.nightking.hotelmanagement.entity.AccountRole;
import org.nightking.hotelmanagement.gateway.AccountRepository;
import org.nightking.hotelmanagement.gateway.PasswordHasher;
import org.nightking.hotelmanagement.model.AccountResponse;
import org.nightking.hotelmanagement.model.RegisterGuestRequest;

public class RegisterGuestInteractor implements RegisterGuestUseCase {
    static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{3,50}");

    private final AccountRepository accounts;
    private final PasswordHasher passwordHasher;

    public RegisterGuestInteractor(AccountRepository accounts, PasswordHasher passwordHasher) {
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void execute(RegisterGuestRequest request, AccountPresenter presenter) {
        String problem = validate(request);
        if (problem != null) {
            presenter.presentInvalidRequest(problem);
            return;
        }
        if (accounts.findByUsername(request.username()).isPresent()) {
            presenter.presentRejected("Username " + request.username() + " is already taken.");
            return;
        }

        Account account = new Account(UUID.randomUUID().toString(), request.username(),
                passwordHasher.hash(request.password()), AccountRole.GUEST,
                request.name().trim(), request.email().trim(), request.phone().trim());
        accounts.save(account);
        presenter.presentRegistered(AccountResponse.from(account));
    }

    private static String validate(RegisterGuestRequest request) {
        if (request.username() == null || !USERNAME.matcher(request.username()).matches()) {
            return "Username must be 3-50 characters: letters, digits, dot, dash or underscore.";
        }
        if (request.password() == null || request.password().length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.";
        }
        if (isBlank(request.name()) || isBlank(request.phone())) {
            return "Name and phone are required.";
        }
        if (isBlank(request.email()) || !request.email().contains("@")) {
            return "A valid email address is required.";
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
