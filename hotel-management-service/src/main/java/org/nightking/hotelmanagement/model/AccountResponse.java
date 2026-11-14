package org.nightking.hotelmanagement.model;

import org.nightking.hotelmanagement.entity.Account;

/** An account without its password hash. */
public record AccountResponse(String id, String username, String role, String name, String email, String phone) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getUsername(), account.getRole().name(),
                account.getName(), account.getEmail(), account.getPhone());
    }
}
