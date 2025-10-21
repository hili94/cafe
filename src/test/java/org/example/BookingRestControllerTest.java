package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingRestController.class)
class BookingRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingRepository bookingRepository;

    // Helper method to create a valid booking
    private Booking createValidBooking() {
        Booking booking = new Booking();
        booking.setCustomerName("John Doe");
        booking.setEmail("johndoe@gmail.com");
        booking.setPhone("1112223456");
        booking.setReservationDate(LocalDate.now().plusDays(7));
        booking.setReservationTime(LocalTime.parse("10:00:00"));
        booking.setNumberOfGuests(4);
        return booking;
    }

    @Test
    void testGetAllBookings() throws Exception {
        // Arrange
        Booking booking1 = createValidBooking();
        booking1.setId(1L);

        Booking booking2 = createValidBooking();
        booking2.setId(2L);
        booking2.setCustomerName("Jane Smith");

        when(bookingRepository.findAll()).thenReturn(Arrays.asList(booking1, booking2));

        // Act & Assert
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].customerName", is("John Doe")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].customerName", is("Jane Smith")));

        verify(bookingRepository, times(1)).findAll();
    }

    @Test
    void testGetBookingById_Success() throws Exception {
        // Arrange
        Booking booking = createValidBooking();
        booking.setId(1L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // Act & Assert
        mockMvc.perform(get("/api/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.customerName", is("John Doe")))
                .andExpect(jsonPath("$.email", is("johndoe@gmail.com")))
                .andExpect(jsonPath("$.numberOfGuests", is(4)));

        verify(bookingRepository, times(1)).findById(1L);
    }

    @Test
    void testGetBookingById_NotFound() throws Exception {
        // Arrange
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/bookings/999"))
                .andExpect(status().isNotFound());

        verify(bookingRepository, times(1)).findById(999L);
    }

    @Test
    void testCreateBooking_Success() throws Exception {
        // Arrange
        Booking booking = createValidBooking();
        Booking savedBooking = createValidBooking();
        savedBooking.setId(1L);

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.customerName", is("John Doe")))
                .andExpect(jsonPath("$.email", is("johndoe@gmail.com")));

        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_EmptyName() throws Exception {
        // Arrange
        Booking booking = createValidBooking();
        booking.setCustomerName("");

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isBadRequest());

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_InvalidEmail() throws Exception {
        // Arrange
        Booking booking = createValidBooking();
        booking.setEmail("johndoe");

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isBadRequest());

        verify(bookingRepository, never()).save(any(Booking.class));
    }
    @Test
    void testCreateBooking_TooManyGuests() throws Exception {
        // Arrange
        Booking booking = createValidBooking();
        booking.setNumberOfGuests(15);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isBadRequest());

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testDeleteBooking_Success() throws Exception {
        // Arrange
        Booking booking = createValidBooking();
        booking.setId(1L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        doNothing().when(bookingRepository).delete(booking);

        // Act & Assert
        mockMvc.perform(delete("/api/bookings/1"))
                .andExpect(status().isNoContent());

        verify(bookingRepository, times(1)).findById(1L);
        verify(bookingRepository, times(1)).delete(booking);
    }

    @Test
    void testDeleteBooking_NotFound() throws Exception {
        // Arrange
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(delete("/api/bookings/999"))
                .andExpect(status().isNotFound());

        verify(bookingRepository, times(1)).findById(999L);
        verify(bookingRepository, never()).delete(any(Booking.class));
    }

    @Test
    void testAvailableTimeSlots_ExistingBooking() throws Exception {
        // Arrange
        Booking booking = createValidBooking();
        booking.setId(1L);

        // reset here for easy reading
        booking.setReservationTime(LocalTime.parse("10:00:00"));
        booking.setNumberOfGuests(4); // 4 guests = 60 minutes (4 * 15)

        // Mock repository to return this booking when searching by date
        when(bookingRepository.findByReservationDate(booking.getReservationDate()))
                .thenReturn(Arrays.asList(booking));

        // Act & Assert
        // The booking at 10:00 with 4 guests blocks 10:00-11:00
        // So available slots should be: 09:00, 09:15, 09:30, 09:45
        // (10:00, 10:15, 10:30, 10:45 are blocked)
        mockMvc.perform(get("/api/bookings/available-times/" 
                        + booking.getReservationDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(28))) // 4 available slots
                .andExpect(jsonPath("$[0]", is("09:00:00")))
                .andExpect(jsonPath("$[1]", is("09:15:00")))
                .andExpect(jsonPath("$[2]", is("09:30:00")))
                .andExpect(jsonPath("$[3]", is("09:45:00")));

        verify(bookingRepository, times(1)).findByReservationDate(booking.getReservationDate());
    }

    @Test
    void testCreateBooking_TooLateForClosingTime() throws Exception {
        // Arrange - Try to book at 16:45 with 3 guests (needs 45 min, would end at 17:30)
        Booking booking = createValidBooking();
        booking.setReservationTime(LocalTime.of(16, 45));
        booking.setNumberOfGuests(3);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        containsString("cannot be completed before closing time")));

        verify(bookingRepository, never()).save(any(Booking.class));
    }


    @Test
    void testCreateBooking_JustBeforeClosing_Valid() throws Exception {
        // Arrange - Book at 16:45 with 1 guest (needs 15 min, ends at 17:00) - should work
        Booking booking = createValidBooking();
        booking.setReservationTime(LocalTime.of(16, 45));
        booking.setNumberOfGuests(1);
        booking.setId(1L);

        when(bookingRepository.findByReservationDateAndReservationTime(any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_MaxGuests_TooLate() throws Exception {
        // Arrange - Try to book max guests (9) at 15:00 (needs 135 min, would end at 17:15)
        Booking booking = createValidBooking();
        booking.setReservationTime(LocalTime.of(15, 0));
        booking.setNumberOfGuests(9);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        containsString("cannot be completed before closing time")));

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_MaxGuests_JustInTime() throws Exception {
        // Arrange - Book max guests (9) at 14:45 (needs 135 min, ends exactly at 17:00)
        Booking booking = createValidBooking();
        booking.setReservationTime(LocalTime.of(14, 45));
        booking.setNumberOfGuests(9);
        booking.setId(1L);

        when(bookingRepository.findByReservationDateAndReservationTime(any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void testUpdateBooking_TooLateForClosingTime() throws Exception {
        // Arrange
        Booking existingBooking = createValidBooking();
        existingBooking.setId(1L);
        existingBooking.setReservationTime(LocalTime.of(10, 0));
        existingBooking.setNumberOfGuests(2);

        Booking updatedBooking = createValidBooking();
        updatedBooking.setReservationTime(LocalTime.of(16, 45));
        updatedBooking.setNumberOfGuests(3); // Would end at 17:30

        when(bookingRepository.findById(1L)).thenReturn(java.util.Optional.of(existingBooking));

        // Act & Assert
        mockMvc.perform(put("/api/bookings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        containsString("cannot be completed before closing time")));

        verify(bookingRepository, never()).save(any(Booking.class));
    }

}