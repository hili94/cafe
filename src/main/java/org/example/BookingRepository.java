package org.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByReservationDate(LocalDate date);
    List<Booking> findByCustomerName(String customerName);
    List<Booking> findByEmail(String email);
    List<Booking> findByPhone(String phone);
    List<Booking> findByReservationDateAndReservationTime(LocalDate date, LocalTime time);
}
