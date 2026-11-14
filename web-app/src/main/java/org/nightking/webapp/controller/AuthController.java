package org.nightking.webapp.controller;

import org.nightking.webapp.gateway.HotelApiException;
import org.nightking.webapp.gateway.HotelManagementGateway;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/** Log-in page (handled by Spring Security) and guest registration. */
@Controller
public class AuthController {
    private final HotelManagementGateway hotel;

    public AuthController(HotelManagementGateway hotel) {
        this.hotel = hotel;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegistrationForm registrationForm, Model model) {
        try {
            hotel.register(registrationForm.toRegistration());
        } catch (HotelApiException e) {
            registrationForm.setPassword(null);
            model.addAttribute("error", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }
}
