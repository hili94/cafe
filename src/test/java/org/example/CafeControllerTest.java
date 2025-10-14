package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CafeControllerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private Model model;

    @InjectMocks
    private CafeController cafeController;

    // Helper method to create a valid booking
    private Booking createFirstValidBooking() {
        LocalDate today = LocalDate.parse("2021-01-01");
        LocalTime now = LocalTime.parse("10:00:00");

        Booking booking = new Booking();
        booking.setCustomerName("John Doe");
        booking.setEmail("johndoe@gmail.com");
        booking.setPhone("1112223456");
        booking.setReservationDate(today);
        booking.setReservationTime(now);
        booking.setNumberOfGuests(1);

        return booking;
    }

    @Test
    void testBookingGettersAndSetters() {
        Booking booking = new Booking();
        booking.setCustomerName("Johnny");
        String name = booking.getCustomerName();
        assertEquals("Johnny", name);

        booking.setEmail("");
        String email = booking.getEmail();
        assertEquals("", email);

        booking.setPhone("1234567890");
        String phone = booking.getPhone();
        assertEquals("1234567890", phone);

        LocalDate today = LocalDate.parse("2021-01-01");
        booking.setReservationDate(today);
        LocalDate date = booking.getReservationDate();
        assertEquals(today, date);

        LocalTime now = LocalTime.parse("10:00:00");
        booking.setReservationTime(now);
        LocalTime time = booking.getReservationTime();
        assertEquals(now, time);

        booking.setNumberOfGuests(1);
        int guests = booking.getNumberOfGuests();
        assertEquals(1, guests);
    }

    @Test
    void testSubmitBooking_Success() {
        // Arrange
        Booking booking = new Booking();
        booking = createFirstValidBooking();
        
        Booking savedBooking = new Booking();
        savedBooking= createFirstValidBooking();
        savedBooking.setId(1L);
        
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // Act
        String result = cafeController.submitBooking(booking, model);

        // Assert
        assertEquals("redirect:/reservations/confirmation/1", result);
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void testSubmitBooking_DatabaseFailure() {
        // Arrange
        Booking booking = new Booking();
        booking = createFirstValidBooking();
        
        // Simulate database failure
        when(bookingRepository.save(any(Booking.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        String result = cafeController.submitBooking(booking, model);

        // Assert
        assertEquals("booking-form", result);
        verify(model, times(1)).addAttribute(eq("error"), eq("Unable to save booking. Please try again."));
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void testSubmitBooking_InvalidInput() {
        Booking booking = new Booking();
        booking.setCustomerName("");

        String result = cafeController.submitBooking(booking, model);

        assertEquals("booking-form", result);
        verify(model, times(1)).addAttribute(eq("error"), eq("Please enter a valid customer name."));
        verify(bookingRepository, never()).save(any(Booking.class));

        booking.setCustomerName("John");
        booking.setEmail("");

        result = cafeController.submitBooking(booking, model);
        assertEquals("booking-form", result);
        verify(model, times(1)).addAttribute(eq("error"), eq("Please enter a valid email address."));
        verify(bookingRepository, never()).save(any(Booking.class));

        booking.setEmail("sample@gmail.com");
        booking.setPhone("");
        result = cafeController.submitBooking(booking, model);
        assertEquals("booking-form", result);
        verify(model, times(1)).addAttribute(eq("error"), eq("Please enter a valid phone number."));
        verify(bookingRepository, never()).save(any(Booking.class));

        booking.setPhone("1234567890");
        booking.setReservationDate(null);
        result = cafeController.submitBooking(booking, model);
        assertEquals("booking-form", result);
        verify(model, times(1)).addAttribute(eq("error"), eq("Please enter a valid reservation date."));
        verify(bookingRepository, never()).save(any(Booking.class));

        booking.setReservationDate(LocalDate.now());
        booking.setReservationTime(null);
        result = cafeController.submitBooking(booking, model);
        assertEquals("booking-form", result);
        verify(model, times(1)).addAttribute(eq("error"), eq("Please enter a valid reservation time."));
        verify(bookingRepository, never()).save(any(Booking.class));

        booking.setReservationTime(LocalTime.now());
        booking.setNumberOfGuests(0);
        result = cafeController.submitBooking(booking, model);
        assertEquals("booking-form", result);
        verify(model, times(1)).addAttribute(eq("error"), eq("Please enter a valid number of guests."));
        verify(bookingRepository, never()).save(any(Booking.class));

        booking.setNumberOfGuests(10);
        result = cafeController.submitBooking(booking, model);
        assertEquals("booking-form", result);
        verify(model, times(1)).addAttribute(eq("error"), eq("For parties larger than 10 please contact us."));

    }
}
