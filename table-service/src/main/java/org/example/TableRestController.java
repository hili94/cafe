package org.example;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
public class TableRestController {

    private final TableRepository tableRepository;

    public TableRestController(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    /**
     * Find an available table for the given criteria
     * @param numberOfGuests Number of guests
     * @param date Reservation date
     * @param time Reservation time
     * @param durationMinutes Duration of the booking
     * @return Available table number or error response
     */
    @GetMapping("/find-available")
    public ResponseEntity<?> findAvailableTable(
            @RequestParam int numberOfGuests,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam long durationMinutes) {

        // TODO: Implement logic to check table availability
        // This would query existing bookings and find a suitable table
        
        Integer assignedTable = findSuitableTable(numberOfGuests);
        
        if (assignedTable == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No tables available for " + numberOfGuests + " guest(s)");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
        
        Map<String, Integer> response = new HashMap<>();
        response.put("tableNumber", assignedTable);
        return ResponseEntity.ok(response);
    }

    private Integer findSuitableTable(int numberOfGuests) {
        // Simplified logic - find smallest suitable table
        if (numberOfGuests <= 2) {
            return 1; // Tables 1-3 for 2 guests
        } else if (numberOfGuests <= 6) {
            return 4; // Tables 4-7 for 6 guests
        } else if (numberOfGuests <= 9) {
            return 8; // Tables 8-9 for 9 guests
        }
        return null;
    }
}
