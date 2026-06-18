package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerId(Long customerId);

    List<Booking> findByRoomId(Long roomId);

    List<Booking> findByStatus(String status);

    List<Booking> findByStatusIn(List<String> statuses);

    List<Booking> findByCustomerUsernameOrderByIdDesc(String username);

List<Booking> findByStatusAndPaymentStatusAndCreatedAtBefore(
    String status, String paymentStatus, LocalDateTime createdAt
);

    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.status IN ('PENDING', 'CONFIRMED') " +
           "AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)")
    List<Booking> findConflictingBookings(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    // ✅ MỚI: dùng khi SỬA lại 1 booking PENDING đã tồn tại (quay lại từ trang thanh toán)
    // Loại trừ chính booking đó ra khỏi danh sách trùng lịch, tránh nó tự đụng với mình
    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.id <> :excludeBookingId " +
           "AND b.status IN ('PENDING', 'CONFIRMED') " +
           "AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)")
    List<Booking> findConflictingBookingsExcluding(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeBookingId") Long excludeBookingId
    );

    @Query("SELECT b FROM Booking b WHERE b.status IN ('PENDING', 'CONFIRMED') " +
           "AND b.checkOutDate <= :today")
    List<Booking> findExpiredBookings(@Param("today") LocalDate today);
}