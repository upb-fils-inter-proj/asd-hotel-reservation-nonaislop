package org.nightking.hotelmanagement.presenter;

import org.nightking.hotelmanagement.model.AccountResponse;

public record AccountViewModel(String id, String username, String role, String name, String email, String phone) {

    static AccountViewModel from(AccountResponse account) {
        return new AccountViewModel(account.id(), account.username(), account.role(),
                account.name(), account.email(), account.phone());
    }
}
