package org.example;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequestMapping("/reservations")
public class CafeController {

    private final BookingRepository bookingRepository;

    public CafeController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // GET request - Show the booking form
    @GetMapping("/new")
    public String showBookingForm(Model model) {
        // Create an empty Booking object
        Booking booking = new Booking();

        // Add it to the model so Thymeleaf can access it
        model.addAttribute("booking", booking);

        // Return the name of the HTML template (booking-form.html)
        return "booking-form";
    }

    @PostMapping("/submit")
    public String submitBooking(@ModelAttribute Booking booking, Model model) {
        try {
            // check input validity
            if (booking.getCustomerName() == null || booking.getCustomerName().trim().isEmpty()) {
                model.addAttribute("error", "Please enter a valid customer name.");
                return "booking-form";
            }
            if (booking.getEmail() == null || booking.getEmail().trim().isEmpty()) {
                 model.addAttribute("error", "Please enter a valid email address.");
                 return "booking-form";
            }
            if (booking.getPhone() == null || booking.getPhone().trim().isEmpty()) {
                model.addAttribute("error", "Please enter a valid phone number.");
                return "booking-form";
            }
            if (booking.getReservationDate() == null) {
                model.addAttribute("error", "Please enter a valid reservation date.");
                return "booking-form";
            }
            if (booking.getReservationTime() == null) {
                model.addAttribute("error", "Please enter a valid reservation time.");
                return "booking-form";
            }
            if (booking.getNumberOfGuests() < 1) {
                model.addAttribute("error", "Please enter a valid number of guests.");
                return "booking-form";
            } else if (booking.getNumberOfGuests() >= 10) {
                model.addAttribute("error", "For parties larger than 10 please contact us.");
            }

            Booking savedBooking = bookingRepository.save(booking);
            return "redirect:/reservations/confirmation/" + savedBooking.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Unable to save booking. Please try again.");
            return "booking-form";
        }
    }

    // GET request - Show confirmation page
    @GetMapping("/confirmation/{id}")
    public String showConfirmation(@PathVariable Long id, Model model) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid booking ID: " + id));
    
        model.addAttribute("booking", booking);
        return "booking-confirmation";
    }

}