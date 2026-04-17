package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // Tìm booking theo khách hàng
    List<Booking> findByCustomerId(Long customerId);
    
    // Tìm booking theo phòng
    List<Booking> findByRoomId(Long roomId);
    
    // Tìm booking theo trạng thái
    List<Booking> findByStatus(String status);

    List<Booking> findByStatusIn(List<String> statuses);
    
    // Kiểm tra phòng có bị đặt trong khoảng thời gian không
    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.status NOT IN ('CANCELLED') " +
           "AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)")
    List<Booking> findConflictingBookings(Long roomId, LocalDate checkIn, LocalDate checkOut);
}