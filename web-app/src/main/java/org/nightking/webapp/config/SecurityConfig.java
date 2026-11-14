package org.nightking.webapp.config;

import org.nightking.webapp.gateway.HotelManagementGateway;
import org.nightking.webapp.model.Account;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Guests must register or log in before booking; administration pages need the ADMIN role.
 * Session-based form login with CSRF protection.
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
                        .requestMatchers("/", "/login", "/register", "/css/**", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/reservations/**").hasRole("GUEST")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(redirectByRole())
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .build();
    }

    private static AuthenticationSuccessHandler redirectByRole() {
        return (request, response, authentication) -> {
            boolean admin = authentication.getPrincipal() instanceof Account account && account.isAdmin();
            response.sendRedirect(request.getContextPath() + (admin ? "/admin" : "/reservations"));
        };
    }
}
