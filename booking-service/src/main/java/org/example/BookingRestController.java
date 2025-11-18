package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingRestController {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;

    // DTO for interacting with Table Service
    private record TableDTO(Long id, Long tableSize) {}

    @Value("${table.service.url:http://localhost:8081}")
    private String tableServiceUrl;

    @Autowired
    public BookingRestController(BookingRepository bookingRepository, RestTemplate restTemplate) {
        this.bookingRepository = bookingRepository;
        this.restTemplate = restTemplate;
    }

    // GET all bookings
    @GetMapping
    public List<Booking> getAllBookings(
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String phone) {

        // Filter by email if provided
        if (email != null && !email.isEmpty()) {
            return bookingRepository.findByEmail(email);
        }

        // Filter by phone if provided
        if (phone != null && !phone.isEmpty()) {
            return bookingRepository.findByPhone(phone);
        }

        // Return all bookings if no filters
        return bookingRepository.findAll();
    }

    // GET bookings by email
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getAllBookingsByEmail(@PathVariable String email) {
        List<Booking> bookings = bookingRepository.findByEmail(email);

        if (bookings.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No bookings found with the email");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        return ResponseEntity.status(HttpStatus.OK).body(bookings);
    }

    //GET bookings by phone
    @GetMapping("phone/{phone}")
    public ResponseEntity<?> getAllBookingsByPhone(@PathVariable String phone) {
        List<Booking> bookings = bookingRepository.findByPhone(phone);

        if (bookings.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No bookings found with the phone number");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        return ResponseEntity.ok(bookings);
    }

    // GET a single booking by ID
    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
        return bookingRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // POST - Create a new booking
    @PostMapping
    public ResponseEntity<?> createBooking(@Valid @RequestBody Booking booking) {
        // Spring automatically validates based on annotations
        // If validation fails, it throws MethodArgumentNotValidException

        // Check if booking can be completed before closing time
        String validationError = validateBookingTime(booking);
            if (validationError != null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", validationError);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Logic moved back to BookingService to avoid circular dependency
            // We use local data to check availability instead of calling external service
            Integer assignedTable = assignTable(booking);

            if (assignedTable == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No tables available for " + booking.getNumberOfGuests() +
                        " guest(s) at this time. Please select a different time.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            booking.setTableNumber(assignedTable);

            try {
            Booking savedBooking = bookingRepository.save(booking);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedBooking);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // This catches the race condition where two users try to book simultaneously
            Map<String, String> error = new HashMap<>();
            error.put("error", "This time slot was just booked by another customer. Please select a different time.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }


    // GET - Get list of available time slots for given date
    @GetMapping("/available-times/{date}")
    public ResponseEntity<List<LocalTime>> getAvailableTimes(@PathVariable String date) {
        // get reservations for given date
        //convert date string to LocalDate
        LocalDate localDate = LocalDate.parse(date);
        List<Booking> bookings = bookingRepository.findByReservationDate(localDate);

        // generate available time slots
        List<LocalTime> times = generateTimeSlots(localDate);

        // remove any times that are already booked
        for (Booking booking : bookings) {
            LocalTime startTime = booking.getReservationTime();
            LocalTime endTime = startTime.plusMinutes(booking.getNumberOfGuests() * 15L);
            for (int i = 0; i < times.size(); i++) { // for each timeslot
                LocalTime time = times.get(i); //get the current timeslot
                if ((time.equals(startTime) || time.isAfter(startTime)) && time.isBefore(endTime)) {
                    times.remove(i);
                    i--; //decrement i to skip the current timeslot since it was already removed
                }
            }
        }

        return ResponseEntity.ok(times);
    }

    // PUT - Update an existing booking
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBooking(@PathVariable Long id, @Valid @RequestBody Booking bookingDetails) {
        return bookingRepository.findById(id)
            .map(booking -> {
                // Check if booking can be completed before closing time
                String validationError = validateBookingTime(bookingDetails);
                if (validationError != null) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", validationError);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }

                // Create a temporary booking object to check table availability
                // (excluding the current booking from conflict check)
                Booking tempBooking = new Booking();
                tempBooking.setId(id); // Keep the same ID
                tempBooking.setReservationDate(bookingDetails.getReservationDate());
                tempBooking.setReservationTime(bookingDetails.getReservationTime());
                tempBooking.setNumberOfGuests(bookingDetails.getNumberOfGuests());

                // Try to assign a table (this will check availability)
                Integer assignedTable = assignTableForUpdate(tempBooking, id);
                if (assignedTable == null) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "No tables available for " + bookingDetails.getNumberOfGuests() +
                            " guest(s) at this time. Please select a different time.");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
                }

                booking.setCustomerName(bookingDetails.getCustomerName());
                booking.setEmail(bookingDetails.getEmail());
                booking.setPhone(bookingDetails.getPhone());
                booking.setReservationDate(bookingDetails.getReservationDate());
                booking.setReservationTime(bookingDetails.getReservationTime());
                booking.setNumberOfGuests(bookingDetails.getNumberOfGuests());
                try {
                    Booking updated = bookingRepository.save(booking);
                    return ResponseEntity.ok().body(updated);
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Race condition during update
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "This time slot was just booked by another customer. Please select a different time.");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                    // Optimistic locking failure - someone else modified this booking
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "This booking was modified by another user. Please refresh and try again.");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
                }
            })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE a booking
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        return bookingRepository.findById(id)
            .map(booking -> {
                bookingRepository.delete(booking);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // Exception handler for validation errors (optional but recommended)
    //TODO make unique error messages for each field
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }

    private boolean checkForConflictingBookings(Booking booking) {
        List<Booking> bookings = bookingRepository.findByReservationDateAndReservationTime(booking.getReservationDate(), booking.getReservationTime());
        return !bookings.isEmpty();
    }

    /**
     * Fetch the list of all tables from the Table Service (Inventory)
     */
    private List<TableDTO> fetchTables() {
        String url = tableServiceUrl + "/api/tables";
        try {
            ResponseEntity<List<TableDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<TableDTO>>() {}
            );
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error fetching tables: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Assigns the smallest available table that can accommodate the number of guests.
     * Fetches table configurations dynamically from Table Service.
     *
     * @param booking The booking to assign a table to
     * @return The assigned table number, or null if no suitable table is available
     */
    private Integer assignTable(Booking booking) {
        int numberOfGuests = booking.getNumberOfGuests();
        LocalDate date = booking.getReservationDate();
        LocalTime time = booking.getReservationTime();

        // Calculate booking duration
        long durationMinutes = numberOfGuests * 15L;
        LocalTime bookingEndTime = time.plusMinutes(durationMinutes);

        // Get table inventory
        List<TableDTO> allTables = fetchTables();

        // Sort tables by size (ascending) to try smallest suitable table first
        allTables.sort(Comparator.comparingLong(TableDTO::tableSize));

        // Get all bookings for this date
        List<Booking> existingBookings = bookingRepository.findByReservationDate(date);

        for (TableDTO table : allTables) {
            // Check if table is large enough
            if (table.tableSize() >= numberOfGuests) {
                // Check if table is free
                if (isTableAvailable(table.id().intValue(), time, bookingEndTime, existingBookings)) {
                    return table.id().intValue();
                }
            }
        }

        // No available table found
        return null;
    }

    /**
     * Checks if a specific table is available for the given time period.
     * A table is available if there are no overlapping bookings.
     *
     * @param tableNumber The table number to check
     * @param startTime The requested booking start time
     * @param endTime The requested booking end time
     * @param existingBookings All bookings for the requested date
     * @return true if the table is available, false otherwise
     */
    private boolean isTableAvailable(int tableNumber, LocalTime startTime, LocalTime endTime,
                                     List<Booking> existingBookings) {
        for (Booking existing : existingBookings) {
            // Skip bookings for different tables
            if (existing.getTableNumber() == null || existing.getTableNumber() != tableNumber) {
                continue;
            }

            // Calculate existing booking's time range
            LocalTime existingStart = existing.getReservationTime();
            long existingDuration = existing.getNumberOfGuests() * 15L;
            LocalTime existingEnd = existingStart.plusMinutes(existingDuration);

            // Check for overlap
            // Overlap occurs if: (startTime < existingEnd) AND (endTime > existingStart)
            if (startTime.isBefore(existingEnd) && endTime.isAfter(existingStart)) {
                return false; // Table is occupied during this time
            }
        }

        return true; // Table is available
    }

    /**
     * Assigns a table for an existing booking update, excluding the booking being updated
     * from conflict checking.
     *
     * @param booking The booking with updated details
     * @param excludeBookingId The ID of the booking being updated (to exclude from conflicts)
     * @return The assigned table number, or null if no suitable table is available
     */
    private Integer assignTableForUpdate(Booking booking, Long excludeBookingId) {
        int numberOfGuests = booking.getNumberOfGuests();
        LocalDate date = booking.getReservationDate();
        LocalTime time = booking.getReservationTime();

        // Calculate booking duration
        long durationMinutes = numberOfGuests * 15L;
        LocalTime bookingEndTime = time.plusMinutes(durationMinutes);

        // Get table inventory
        List<TableDTO> allTables = fetchTables();
        allTables.sort(Comparator.comparingLong(TableDTO::tableSize));

        // Get all bookings for this date, excluding the one being updated
        List<Booking> existingBookings = bookingRepository.findByReservationDate(date)
                .stream()
                .filter(b -> !b.getId().equals(excludeBookingId))
                .toList();

        for (TableDTO table : allTables) {
            if (table.tableSize() >= numberOfGuests) {
                if (isTableAvailable(table.id().intValue(), time, bookingEndTime, existingBookings)) {
                    return table.id().intValue();
                }
            }
        }

        return null;
    }

    // Validates that a booking can be completed before the cafe closes.
    // Each guest requires 15 minutes, and cafe closes at 17:00.
    private String validateBookingTime(Booking booking) {
        LocalTime closingTime = LocalTime.of(17, 0);
        LocalTime reservationTime = booking.getReservationTime();
        int numberOfGuests = booking.getNumberOfGuests();

        // Calculate how long the booking will take (15 minutes per guest)
        long durationMinutes = numberOfGuests * 15L;

        // Calculate when the booking would end
        LocalTime bookingEndTime = reservationTime.plusMinutes(durationMinutes);

        // Check if booking would end after closing time
        if (bookingEndTime.isAfter(closingTime)) {
            return String.format(
                    "Booking cannot be completed before closing time (17:00). " +
                            "A reservation at %s for %d guest%s would require until %s.",
                    reservationTime.toString(),
                    numberOfGuests,
                    numberOfGuests == 1 ? "" : "s",
                    bookingEndTime.toString()
            );
        }

        return null; // Valid booking
    }

    // Generate a list of LocalTime objects from 9:00 to 17:00 in 15-minute increments
    private static List<LocalTime> generateTimeSlots(LocalDate date) {
        List<LocalTime> timeList = new ArrayList<>();
        LocalTime currentTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(17, 0);
        long incrementValue = 15;

        while (!currentTime.isAfter(endTime)) {
            timeList.add(currentTime);
            currentTime = currentTime.plusMinutes(incrementValue);
        }
        // if today's date is equal to date, remove time slots before current time
        if (date.isEqual(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            timeList.removeIf(time -> time.isBefore(now));
        }
        //remove final timeslot as this is too late in the cafe's day to take guests
        timeList.remove(timeList.size() - 1);
        return timeList;
    }
}
