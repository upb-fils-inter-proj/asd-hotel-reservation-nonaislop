package org.nightking.webapp.controller;

import org.nightking.webapp.gateway.HotelApiException;
import org.nightking.webapp.gateway.HotelManagementGateway;
import org.nightking.webapp.model.Account;
import org.nightking.webapp.model.Reservation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** A signed-in guest's own reservations, with cancellation. */
@Controller
@RequestMapping("/reservations")
public class GuestReservationController {
    private final HotelManagementGateway hotel;

    public GuestReservationController(HotelManagementGateway hotel) {
        this.hotel = hotel;
    }

    @GetMapping
    public String myReservations(@AuthenticationPrincipal Account guest, Model model) {
        model.addAttribute("reservations", hotel.reservationsOf(guest.id()).stream()
                .filter(reservation -> !reservation.wasNeverPaid())
                .toList());
        return "reservations";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable String id, @AuthenticationPrincipal Account guest, RedirectAttributes redirect) {
        try {
            Reservation cancelled = hotel.cancel(id, guest.id());
            redirect.addFlashAttribute("message", "Reservation " + cancelled.reservationId() + " is cancelled. "
                    + cancelled.currency() + " " + cancelled.totalPrice() + " has been refunded.");
        } catch (HotelApiException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservations";
    }
}
