package org.nightking.hotelmanagement.gateway;

import java.util.Optional;
import org.nightking.hotelmanagement.entity.Account;

public interface AccountRepository {
    Optional<Account> findById(String accountId);
    Optional<Account> findByUsername(String username);
    void save(Account account);
}
