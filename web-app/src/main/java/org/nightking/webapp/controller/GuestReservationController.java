package org.nightking.webapp.controller;

import org.nightking.webapp.gateway.HotelApiException;
import org.nightking.webapp.gateway.HotelManagementGateway;
import org.nightking.webapp.model.Account;
import org.nightking.webapp.model.Reservation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pages for signed-in guests: book a room and see their own reservations. */
@Controller
@RequestMapping("/reservations")
public class GuestReservationController {
    private final HotelManagementGateway hotel;

    public GuestReservationController(HotelManagementGateway hotel) {
        this.hotel = hotel;
    }

    @GetMapping
    public String myReservations(@AuthenticationPrincipal Account guest, Model model) {
        model.addAttribute("reservations", hotel.reservationsOf(guest.id()));
        return "reservations";
    }

    @GetMapping("/new")
    public String bookingForm(Model model) {
        model.addAttribute("bookingForm", new BookingForm());
        model.addAttribute("rooms", hotel.rooms());
        return "book";
    }

    @PostMapping
    public String book(@AuthenticationPrincipal Account guest, @ModelAttribute BookingForm bookingForm,
                       Model model, RedirectAttributes redirect) {
        Reservation reservation;
        try {
            // The guest id always comes from the signed-in session, never from the form.
            reservation = hotel.book(bookingForm.toRequest(guest.id()));
        } catch (HotelApiException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("rooms", hotel.rooms());
            return "book";
        }
        redirect.addFlashAttribute("message", "Reservation " + reservation.reservationId() + " is "
                + reservation.state() + ". Total: " + reservation.currency() + " " + reservation.totalPrice() + ".");
        return "redirect:/reservations";
    }
}
