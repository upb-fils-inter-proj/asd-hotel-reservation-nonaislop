package org.nightking.hotelmanagement.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class AccountJpaEntity {
    @Id
    private String id;
    private String username;
    private String passwordHash;
    private String role;
    private String name;
    private String email;
    private String phone;

    protected AccountJpaEntity() {
    }

    AccountJpaEntity(String id, String username, String passwordHash, String role,
                     String name, String email, String phone) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    String getId() { return id; }
    String getUsername() { return username; }
    String getPasswordHash() { return passwordHash; }
    String getRole() { return role; }
    String getName() { return name; }
    String getEmail() { return email; }
    String getPhone() { return phone; }
}
