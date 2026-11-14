package org.nightking.hotelmanagement.infrastructure;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.nightking.hotelmanagement.entity.Account;
import org.nightking.hotelmanagement.gateway.AccountRepository;

/** In-memory AccountRepository, used by the fast use-case tests. */
public class InMemoryAccountRepository implements AccountRepository {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findById(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        return accounts.values().stream().filter(a -> a.getUsername().equals(username)).findFirst();
    }

    @Override
    public void save(Account account) {
        accounts.put(account.getId(), account);
    }
}
