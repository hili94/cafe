
package org.example;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String home() {
        return "redirect:/booking_form.html";
    }
    
    @GetMapping("/reservations")
    public String reservations() {
        return "redirect:/booking_form.html";
    }
    
    @GetMapping("/admin")
    public String admin() {
        return "redirect:/admin_dashboard.html";
    }
}
