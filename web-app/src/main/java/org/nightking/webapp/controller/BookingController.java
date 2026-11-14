package org.nightking.webapp.controller;

import java.time.Year;
import org.nightking.webapp.gateway.HotelApiException;
import org.nightking.webapp.gateway.HotelManagementGateway;
import org.nightking.webapp.model.Account;
import org.nightking.webapp.model.BookingQuote;
import org.nightking.webapp.model.Reservation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The booking flow for signed-in guests: choose extras, review the price, pay. */
@Controller
@RequestMapping("/book/{roomNumber}")
public class BookingController {
    private final HotelManagementGateway hotel;

    public BookingController(HotelManagementGateway hotel) {
        this.hotel = hotel;
    }

    /** Step 1: the room is free for these dates; pick additional services. */
    @GetMapping
    public String extras(@PathVariable String roomNumber, @ModelAttribute BookingForm bookingForm,
                         Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("quote", hotel.quote(bookingForm.toQuote(roomNumber)));
        } catch (HotelApiException e) {
            return backToSearch(bookingForm, e.getMessage(), redirect);
        }
        return "book";
    }

    /** Step 2: show the price breakdown and ask for the card. */
    @PostMapping("/review")
    public String review(@PathVariable String roomNumber, @ModelAttribute BookingForm bookingForm,
                         Model model, RedirectAttributes redirect) {
        try {
            showReview(model, hotel.quote(bookingForm.toQuote(roomNumber)));
        } catch (HotelApiException e) {
            return backToSearch(bookingForm, e.getMessage(), redirect);
        }
        return "review";
    }

    /** Step 3: book and pay. A declined card brings the guest back to the review page. */
    @PostMapping("/confirm")
    public String confirm(@PathVariable String roomNumber, @AuthenticationPrincipal Account guest,
                          @ModelAttribute BookingForm bookingForm, Model model, RedirectAttributes redirect) {
        Reservation reservation;
        try {
            // The guest id always comes from the signed-in session, never from the form.
            reservation = hotel.book(bookingForm.toBooking(guest.id(), roomNumber));
        } catch (HotelApiException e) {
            bookingForm.forgetCard();
            try {
                showReview(model, hotel.quote(bookingForm.toQuote(roomNumber)));
            } catch (HotelApiException taken) {
                return backToSearch(bookingForm, taken.getMessage(), redirect);
            }
            model.addAttribute("error", e.getMessage());
            return "review";
        }
        redirect.addFlashAttribute("message", "Payment approved (" + reservation.paidWith() + "). Reservation "
                + reservation.reservationId() + " is confirmed: room " + reservation.roomNumber() + ", "
                + reservation.checkIn() + " to " + reservation.checkOut() + ", "
                + reservation.currency() + " " + reservation.totalPrice() + ".");
        return "redirect:/reservations";
    }

    private static void showReview(Model model, BookingQuote quote) {
        model.addAttribute("quote", quote);
        model.addAttribute("currentYear", Year.now().getValue());
    }

    private static String backToSearch(BookingForm form, String message, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", message);
        if (form.getCheckIn() == null || form.getCheckOut() == null) {
            return "redirect:/";
        }
        return "redirect:/rooms/available?checkIn=" + form.getCheckIn() + "&checkOut=" + form.getCheckOut();
    }
}
