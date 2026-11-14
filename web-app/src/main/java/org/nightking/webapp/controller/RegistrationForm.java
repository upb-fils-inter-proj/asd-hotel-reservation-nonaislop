package org.nightking.webapp.controller;

import org.nightking.webapp.model.Registration;

/** Backing object of the registration page. */
public class RegistrationForm {
    private String username;
    private String password;
    private String name;
    private String email;
    private String phone;

    Registration toRegistration() {
        return new Registration(username, password, name, email, phone);
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
