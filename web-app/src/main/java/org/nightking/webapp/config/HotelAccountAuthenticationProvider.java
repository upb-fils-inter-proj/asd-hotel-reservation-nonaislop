package org.nightking.webapp.config;

import java.util.List;
import org.nightking.webapp.gateway.HotelManagementGateway;
import org.nightking.webapp.model.Account;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Checks the login form against the accounts kept by the hotel management service.
 * The signed-in principal is the {@link Account}; its role becomes ROLE_GUEST or ROLE_ADMIN.
 */
public class HotelAccountAuthenticationProvider implements AuthenticationProvider {
    private final HotelManagementGateway hotel;

    public HotelAccountAuthenticationProvider(HotelManagementGateway hotel) {
        this.hotel = hotel;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());
        Account account;
        try {
            account = hotel.authenticate(username, password)
                    .orElseThrow(() -> new BadCredentialsException("Invalid username or password."));
        } catch (BadCredentialsException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AuthenticationServiceException("The hotel service is not reachable right now.", e);
        }
        return UsernamePasswordAuthenticationToken.authenticated(account, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.role())));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
