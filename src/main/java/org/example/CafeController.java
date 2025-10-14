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
        // At this point, Spring has automatically filled the 'booking' object
        // with data from the form using the getters/setters

        // You could save to database here, send email, etc.
        // For now, we'll just pass it to the confirmation page

        System.out.println("Received booking for: " + booking.getCustomerName());

        // Add the booking to the model for the confirmation page
        model.addAttribute("booking", booking);

        // Return the confirmation page template
        return "booking-confirmation";
    }
}