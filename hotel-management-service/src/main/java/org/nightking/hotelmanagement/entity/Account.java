package org.nightking.hotelmanagement.entity;

/** Someone who can sign in: a registered guest, or a pre-created administrator. */
public class Account {
    private final String id;
    private final String username;
    private String passwordHash;
    private final AccountRole role;
    private final String name;
    private final String email;
    private final String phone;

    public Account(String id, String username, String passwordHash, AccountRole role,
                   String name, String email, String phone) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public AccountRole getRole() { return role; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    public void changePasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** The guest details a reservation is made under. */
    public Guest asGuest() { return new Guest(id, name, email, phone); }
}
