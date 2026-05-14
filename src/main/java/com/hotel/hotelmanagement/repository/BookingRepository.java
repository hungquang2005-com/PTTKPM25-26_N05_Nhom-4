package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerId(Long customerId);

    List<Booking> findByRoomId(Long roomId);

    List<Booking> findByStatus(String status);

    List<Booking> findByStatusIn(List<String> statuses);

    List<Booking> findByCustomerUsernameOrderByIdDesc(String username);

    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.status IN ('PENDING', 'CONFIRMED') " +
           "AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)")
    List<Booking> findConflictingBookings(Long roomId, LocalDate checkIn, LocalDate checkOut);
}