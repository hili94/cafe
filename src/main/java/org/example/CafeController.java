package org.example;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    // POST request - Process the submitted form
    @PostMapping("/submit")
    public String submitBooking(@ModelAttribute Booking booking, Model model) {
        // Save the booking to the database
        Booking savedBooking = bookingRepository.save(booking);

        System.out.println("Saved booking to database with ID: " + savedBooking.getId());
        System.out.println("Received booking for: " + booking.getCustomerName());

        // Add the booking to the model for the confirmation page
        model.addAttribute("booking", savedBooking);

        // Return the confirmation page template
        return "booking-confirmation";
    }
}