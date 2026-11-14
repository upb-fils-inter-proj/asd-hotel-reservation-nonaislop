package org.nightking.webapp.controller;

import java.time.LocalDate;
import java.util.List;
import org.nightking.webapp.gateway.HotelApiException;
import org.nightking.webapp.gateway.HotelManagementGateway;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;

/** Public pages: the rooms and the availability search. */
@Controller
public class HomeController {
    private final HotelManagementGateway hotel;

    public HomeController(HotelManagementGateway hotel) {
        this.hotel = hotel;
    }

    @GetMapping("/")
    public String home(Model model) {
        addSearchDefaults(model, null, null);
        try {
            model.addAttribute("rooms", hotel.rooms());
        } catch (RestClientException e) {
            model.addAttribute("rooms", List.of());
            model.addAttribute("error", "Rooms can't be loaded right now. Please try again shortly.");
        }
        return "index";
    }

    @GetMapping("/rooms/available")
    public String available(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                            Model model) {
        addSearchDefaults(model, checkIn, checkOut);
        try {
            model.addAttribute("availability", hotel.availableRooms(checkIn, checkOut));
        } catch (HotelApiException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "availability";
    }

    private static void addSearchDefaults(Model model, LocalDate checkIn, LocalDate checkOut) {
        LocalDate today = LocalDate.now();
        model.addAttribute("today", today);
        model.addAttribute("checkIn", checkIn != null ? checkIn : today.plusDays(1));
        model.addAttribute("checkOut", checkOut != null ? checkOut : today.plusDays(3));
    }
}
