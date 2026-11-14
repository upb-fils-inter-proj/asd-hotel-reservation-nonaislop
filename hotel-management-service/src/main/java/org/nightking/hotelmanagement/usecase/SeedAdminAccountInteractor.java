package org.nightking.hotelmanagement.usecase;

import java.util.Optional;
import java.util.UUID;
import org.nightking.hotelmanagement.entity.Account;
import org.nightking.hotelmanagement.entity.AccountRole;
import org.nightking.hotelmanagement.gateway.AccountRepository;
import org.nightking.hotelmanagement.gateway.PasswordHasher;

public class SeedAdminAccountInteractor implements SeedAdminAccountUseCase {
    private final AccountRepository accounts;
    private final PasswordHasher passwordHasher;

    public SeedAdminAccountInteractor(AccountRepository accounts, PasswordHasher passwordHasher) {
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void execute(String username, String password) {
        Optional<Account> existing = accounts.findByUsername(username);
        if (existing.isEmpty()) {
            accounts.save(new Account(UUID.randomUUID().toString(), username, passwordHasher.hash(password),
                    AccountRole.ADMIN, "Administrator", username + "@hotel.local", "-"));
            System.out.println("Admin account '" + username + "' created.");
            return;
        }
        // The secret is the source of truth: a rotated secret updates the stored hash.
        Account admin = existing.get();
        if (!passwordHasher.matches(password, admin.getPasswordHash())) {
            admin.changePasswordHash(passwordHasher.hash(password));
            accounts.save(admin);
            System.out.println("Admin account '" + username + "' password updated.");
        }
    }
}
