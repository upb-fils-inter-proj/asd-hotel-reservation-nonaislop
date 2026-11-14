package org.nightking.webapp.controller;

import org.nightking.webapp.gateway.HotelApiException;
import org.nightking.webapp.gateway.HotelManagementGateway;
import org.nightking.webapp.model.Reservation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Administration: every reservation, room status, check-in and check-out. */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final HotelManagementGateway hotel;

    public AdminController(HotelManagementGateway hotel) {
        this.hotel = hotel;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("reservations", hotel.allReservations());
        model.addAttribute("rooms", hotel.rooms());
        return "admin";
    }

    @PostMapping("/reservations/{id}/check-in")
    public String checkIn(@PathVariable String id, RedirectAttributes redirect) {
        try {
            Reservation reservation = hotel.checkIn(id);
            redirect.addFlashAttribute("message", reservation.guestName() + " checked in to room "
                    + reservation.roomNumber() + ".");
        } catch (HotelApiException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/reservations/{id}/check-out")
    public String checkOut(@PathVariable String id, RedirectAttributes redirect) {
        try {
            Reservation reservation = hotel.checkOut(id);
            redirect.addFlashAttribute("message", reservation.guestName() + " checked out of room "
                    + reservation.roomNumber() + ".");
        } catch (HotelApiException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }
}
