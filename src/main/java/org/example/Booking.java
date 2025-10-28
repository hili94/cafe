package org.example;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "bookings",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"reservation_date", "reservation_time"},
                name = "uk_booking_date_time"
        ))

public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version // Optimistic locking
    private Long version;

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    @Column(nullable = false)
    private String customerName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(nullable = false)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9\\-\\s()]+$", message = "Phone number must contain only numbers, spaces, dashes, or parentheses")
    @Column(nullable = false)
    private String phone;
    
    @NotNull(message = "Reservation date is required")
    @FutureOrPresent(message = "Reservation date must be in the future")
    @Column(nullable = false)
    private LocalDate reservationDate;
    
    @NotNull(message = "Reservation time is required")
    @Column(nullable = false)
    private LocalTime reservationTime;
    
    @Min(value = 1, message = "At least 1 guest is required")
    @Max(value = 9, message = "For parties of 10 or more, please contact us directly")
    @Column(nullable = false)
    private int numberOfGuests;

    /*     * Table configuration:
     * - Tables 1, 2, 3: up to 2 guests
     * - Tables 4, 5, 6, 7: up to 6 guests
     * - Tables 8, 9: up to 9 guests
     */
    @Column(nullable = true) // Will be assigned by backend
    private Integer tableNumber;

    // Default constructor (needed by Spring)
    public Booking() {
    }

    public Booking(String customerName, String email, String phone, LocalDate reservationDate, LocalTime reservationTime, int numberOfGuests) {
        this.customerName = customerName;
        this.email = email;
        this.phone = phone;
        this.reservationDate = reservationDate;
        this.reservationTime = reservationTime;
        this.numberOfGuests = numberOfGuests;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() { return version; }

    public void setVersion(Long version) { this.version = version; }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDate reservationDate) {
        this.reservationDate = reservationDate;
    }

    public LocalTime getReservationTime() {
        return reservationTime;
    }

    public void setReservationTime(LocalTime reservationTime) {
        this.reservationTime = reservationTime;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public Integer getTableNumber() { return tableNumber; }

    public void setTableNumber(Integer tableNumber) { this.tableNumber = tableNumber; }
}
