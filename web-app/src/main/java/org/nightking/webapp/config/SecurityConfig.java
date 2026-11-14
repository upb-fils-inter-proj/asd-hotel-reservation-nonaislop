package org.nightking.webapp.config;

import org.nightking.webapp.gateway.HotelManagementGateway;
import org.nightking.webapp.model.Account;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

/**
 * Anyone can see the rooms and search availability; booking needs a signed-in guest and the
 * administration pages need the ADMIN role. Session-based form login with CSRF protection.
 */
@Configuration
public class SecurityConfig {

    @Bean
    AuthenticationProvider hotelAccounts(HotelManagementGateway hotel) {
        return new HotelAccountAuthenticationProvider(hotel);
    }

    @Bean
    SecurityFilterChain webSecurity(HttpSecurity http, AuthenticationProvider hotelAccounts) throws Exception {
        return http
                .authenticationProvider(hotelAccounts)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/rooms/**", "/login", "/register", "/css/**", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/book/**", "/reservations/**").hasRole("GUEST")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(afterLogin())
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .build();
    }

    /** A guest who picked a room before logging in goes back to it; otherwise each role has a home page. */
    private static AuthenticationSuccessHandler afterLogin() {
        RequestCache requestCache = new HttpSessionRequestCache();
        return (request, response, authentication) -> {
            boolean admin = authentication.getPrincipal() instanceof Account account && account.isAdmin();
            SavedRequest saved = requestCache.getRequest(request, response);
            if (saved != null && !admin) {
                requestCache.removeRequest(request, response);
                response.sendRedirect(saved.getRedirectUrl());
                return;
            }
            response.sendRedirect(request.getContextPath() + (admin ? "/admin" : "/reservations"));
        };
    }
}
