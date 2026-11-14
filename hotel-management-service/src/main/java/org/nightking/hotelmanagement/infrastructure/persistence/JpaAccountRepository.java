package org.nightking.hotelmanagement.infrastructure.persistence;

import java.util.Optional;
import org.nightking.hotelmanagement.entity.Account;
import org.nightking.hotelmanagement.entity.AccountRole;
import org.nightking.hotelmanagement.gateway.AccountRepository;

/** AccountRepository gateway backed by the hotel database. */
public class JpaAccountRepository implements AccountRepository {
    private final AccountJpaRepository accounts;

    public JpaAccountRepository(AccountJpaRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<Account> findById(String accountId) {
        return accounts.findById(accountId).map(JpaAccountRepository::toDomain);
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        return accounts.findByUsername(username).map(JpaAccountRepository::toDomain);
    }

    @Override
    public void save(Account account) {
        accounts.save(new AccountJpaEntity(account.getId(), account.getUsername(), account.getPasswordHash(),
                account.getRole().name(), account.getName(), account.getEmail(), account.getPhone()));
    }

    static Account toDomain(AccountJpaEntity entity) {
        return new Account(entity.getId(), entity.getUsername(), entity.getPasswordHash(),
                AccountRole.valueOf(entity.getRole()), entity.getName(), entity.getEmail(), entity.getPhone());
    }
}
