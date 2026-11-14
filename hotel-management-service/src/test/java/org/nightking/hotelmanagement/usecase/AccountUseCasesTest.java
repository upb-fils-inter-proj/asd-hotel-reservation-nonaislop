package org.nightking.hotelmanagement.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.entity.Account;
import org.nightking.hotelmanagement.entity.AccountRole;
import org.nightking.hotelmanagement.gateway.PasswordHasher;
import org.nightking.hotelmanagement.infrastructure.InMemoryAccountRepository;
import org.nightking.hotelmanagement.model.AccountResponse;
import org.nightking.hotelmanagement.model.AuthenticateRequest;
import org.nightking.hotelmanagement.model.RegisterGuestRequest;

class AccountUseCasesTest {

    static class RecordingPresenter implements AccountPresenter {
        String outcome;
        AccountResponse account;

        @Override public void presentRegistered(AccountResponse a) { outcome = "REGISTERED"; account = a; }
        @Override public void presentAuthenticated(AccountResponse a) { outcome = "AUTHENTICATED"; account = a; }
        @Override public void presentInvalidCredentials(String m) { outcome = "INVALID_CREDENTIALS"; }
        @Override public void presentRejected(String m) { outcome = "REJECTED"; }
        @Override public void presentInvalidRequest(String m) { outcome = "INVALID"; }
    }

    /** Reversible fake hash, so tests can see the stored value is not the raw password. */
    private final PasswordHasher hasher = new PasswordHasher() {
        @Override public String hash(String raw) { return "hashed:" + raw; }
        @Override public boolean matches(String raw, String hash) { return hash.equals("hashed:" + raw); }
    };
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final RegisterGuestUseCase register = new RegisterGuestInteractor(accounts, hasher);
    private final AuthenticateUseCase authenticate = new AuthenticateInteractor(accounts, hasher);
    private final SeedAdminAccountUseCase seedAdmin = new SeedAdminAccountInteractor(accounts, hasher);

    private RecordingPresenter register(String username, String password) {
        RecordingPresenter presenter = new RecordingPresenter();
        register.execute(new RegisterGuestRequest(username, password, "Alex Morgan", "alex@example.com", "+40"), presenter);
        return presenter;
    }

    private RecordingPresenter login(String username, String password) {
        RecordingPresenter presenter = new RecordingPresenter();
        authenticate.execute(new AuthenticateRequest(username, password), presenter);
        return presenter;
    }

    @Test
    void registersGuestWithHashedPassword() {
        RecordingPresenter result = register("alex", "correct-horse");

        assertEquals("REGISTERED", result.outcome);
        assertEquals("GUEST", result.account.role());
        Account stored = accounts.findByUsername("alex").orElseThrow();
        assertNotEquals("correct-horse", stored.getPasswordHash());
    }

    @Test
    void rejectsTakenUsername() {
        register("alex", "correct-horse");
        assertEquals("REJECTED", register("alex", "another-password").outcome);
    }

    @Test
    void rejectsShortPasswordAndBadUsername() {
        assertEquals("INVALID", register("alex", "short").outcome);
        assertEquals("INVALID", register("a", "correct-horse").outcome);
        assertEquals("INVALID", register("has space", "correct-horse").outcome);
    }

    @Test
    void authenticatesWithTheRightPasswordOnly() {
        register("alex", "correct-horse");
        assertEquals("AUTHENTICATED", login("alex", "correct-horse").outcome);
        assertEquals("INVALID_CREDENTIALS", login("alex", "wrong-password").outcome);
        assertEquals("INVALID_CREDENTIALS", login("nobody", "correct-horse").outcome);
    }

    @Test
    void seedsAdministratorOnceAndFollowsTheSecret() {
        seedAdmin.execute("admin", "first-secret");
        seedAdmin.execute("admin", "first-secret");
        RecordingPresenter admin = login("admin", "first-secret");
        assertEquals("AUTHENTICATED", admin.outcome);
        assertEquals("ADMIN", admin.account.role());

        seedAdmin.execute("admin", "rotated-secret");
        assertEquals("INVALID_CREDENTIALS", login("admin", "first-secret").outcome);
        assertEquals("AUTHENTICATED", login("admin", "rotated-secret").outcome);
        assertTrue(accounts.findByUsername("admin").map(a -> a.getRole() == AccountRole.ADMIN).orElse(false));
    }
}
