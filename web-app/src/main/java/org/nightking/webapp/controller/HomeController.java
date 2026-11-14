package org.nightking.webapp.controller;

import org.nightking.webapp.gateway.HotelManagementGateway;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClientException;

@Controller
public class HomeController {
    private final HotelManagementGateway hotel;

    public HomeController(HotelManagementGateway hotel) {
        this.hotel = hotel;
    }

    @GetMapping("/")
    public String home(Model model) {
        try {
            model.addAttribute("rooms", hotel.rooms());
        } catch (RestClientException e) {
            model.addAttribute("rooms", java.util.List.of());
            model.addAttribute("error", "Rooms can't be loaded right now. Please try again shortly.");
        }
        return "index";
    }
}
