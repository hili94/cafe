package org.example;

import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingRestController {

    private final BookingRepository bookingRepository;

    public BookingRestController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // GET all bookings
    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
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

        //Ensure there is no existing booking at the same time
        boolean isExistingBooking = checkForConflictingBookings(booking);
        if (isExistingBooking) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "There is already a booking at this time");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
        Booking savedBooking = bookingRepository.save(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedBooking);
    }

    // GET - Get list of available time slots for given date
    @GetMapping("/available-times/{date}")
    public ResponseEntity<List<LocalTime>> getAvailableTimes(@PathVariable String date) {
        // get reservations for given date
        //convert date string to LocalDate
        LocalDate localDate = LocalDate.parse(date);
        List<Booking> bookings = bookingRepository.findByReservationDate(localDate);

        // generate available time slots
        List<LocalTime> times = generateTimeSlots();

        // remove any times that are already booked
        for (Booking booking : bookings) {
            LocalTime startTime = booking.getReservationTime();
            LocalTime endTime = startTime.plusMinutes(booking.getNumberOfGuests() * 15L);
            for (int i = 0; i < times.size(); i++) { // for each timeslot
                LocalTime time = times.get(i); //get the current timeslot
                if (time.isAfter(startTime) && time.isBefore(endTime)) { // if the timeslot overlaps with the booking
                    times.remove(i);
                    i--; //decrement i to skip the current timeslot since it was already removed
                }
            }
        }

        return ResponseEntity.ok(times);
    }

    // GET - Get bookings by email
    @GetMapping("/email/{email}")
    public ResponseEntity<List<Booking>> getBookingsByEmail(@PathVariable String email) {
        List<Booking> bookings = bookingRepository.findByEmail(email);

        if (bookings.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(bookings);
    }

    // GET - Get bookings by phone
    @GetMapping("/phone/{phone}")
    public ResponseEntity<List<Booking>> getBookingsByPhone(@PathVariable String phone) {
        List<Booking> bookings = bookingRepository.findByPhone(phone);

        if (bookings.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(bookings);
    }

    // PUT - Update an existing booking
    @PutMapping("/{id}")
    public ResponseEntity<Booking> updateBooking(@PathVariable Long id, @Valid @RequestBody Booking bookingDetails) {
        return bookingRepository.findById(id)
            .map(booking -> {
                booking.setCustomerName(bookingDetails.getCustomerName());
                booking.setEmail(bookingDetails.getEmail());
                booking.setPhone(bookingDetails.getPhone());
                booking.setReservationDate(bookingDetails.getReservationDate());
                booking.setReservationTime(bookingDetails.getReservationTime());
                booking.setNumberOfGuests(bookingDetails.getNumberOfGuests());
                Booking updated = bookingRepository.save(booking);
                return ResponseEntity.ok(updated);
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

    //
    // Generate a list of LocalTime objects from 9:00 to 17:00 in 15-minute increments
    private static List<LocalTime> generateTimeSlots() {
        List<LocalTime> timeList = new ArrayList<>();
        LocalTime currentTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(11, 0);
        long incrementValue = 15;

        while (!currentTime.isAfter(endTime)) {
            timeList.add(currentTime);
            currentTime = currentTime.plusMinutes(incrementValue);
        }
        return timeList;
    }
}
