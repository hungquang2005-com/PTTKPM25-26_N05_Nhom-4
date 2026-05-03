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

    // ✅ Lọc theo username của user đang đăng nhập, mới nhất lên đầu
    List<Booking> findByCustomerUsernameOrderByIdDesc(String username);

    /**
     * Chỉ coi là xung đột khi booking đang ACTIVE thực sự.
     * CONFIRMED + UNPAID  → đang giữ phòng
     * COMPLETED + PAID    → đang ở trong phòng (chưa check-out)
     *
     * KHÔNG tính: CANCELLED, REFUNDED, COMPLETED quá ngày check-out
     * (việc reset phòng về AVAILABLE sau check-out do Scheduler xử lý)
     */
    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.status IN ('CONFIRMED', 'COMPLETED') " +
           "AND b.paymentStatus != 'REFUNDED' " +
           "AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)")
    List<Booking> findConflictingBookings(Long roomId, LocalDate checkIn, LocalDate checkOut);
}