package com.hotel.hotelmanagement.config;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingScheduler {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;

    // ⚠️ KHÔNG inject NotificationService ở đây
    // Scheduler hủy timeout KHÔNG được gửi notification cho admin

    @PostConstruct
    public void runOnStartup() {
        System.out.println("🚀 [Startup] Kiểm tra phòng hết hạn khi khởi động...");
        autoReleaseExpiredRooms();
        cancelUnpaidPendingBookings();
    }

    // ============================================================
    // JOB 1: Hoàn thành booking hết ngày checkOut (00:01 mỗi ngày)
    // ============================================================
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void autoReleaseExpiredRooms() {
        LocalDate today = LocalDate.now();
        System.out.println("⏰ [Scheduler] Kiểm tra phòng hết hạn... Today = " + today);

        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(today);

        if (expiredBookings.isEmpty()) {
            System.out.println("⏰ [Scheduler] Không có phòng nào cần giải phóng.");
            return;
        }

        for (Booking booking : expiredBookings) {
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);

            if (booking.getRoom() != null) {
                List<Booking> futureBookings = bookingRepository.findConflictingBookings(
                        booking.getRoom().getId(), today, today.plusYears(1)
                );
                String newStatus = futureBookings.isEmpty() ? "AVAILABLE" : "BOOKED";
                booking.getRoom().setStatus(newStatus);
                roomRepository.save(booking.getRoom());

                System.out.println("✅ Phòng: " + booking.getRoom().getRoomNumber()
                        + " → " + newStatus
                        + " | Booking #" + booking.getId()
                        + " | CheckOut: " + booking.getCheckOutDate());
            }
        }
        System.out.println("✅ [Scheduler] Tổng giải phóng: " + expiredBookings.size() + " phòng.");
    }

    // ============================================================
    // JOB 2: Hủy booking PENDING chưa thanh toán quá 15 phút
    // ✅ KHÔNG gửi notification — hủy do timeout, không phải đặt thành công
    // ============================================================
    @Scheduled(fixedDelay = 60000) // Chạy mỗi 1 phút
    @Transactional
    public void cancelUnpaidPendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);

        List<Booking> expiredPending = bookingRepository
                .findByStatusAndPaymentStatusAndCreatedAtBefore("PENDING", "UNPAID", cutoff);

        if (expiredPending.isEmpty()) return;

        for (Booking booking : expiredPending) {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);

            if (booking.getRoom() != null) {
                booking.getRoom().setStatus("AVAILABLE");
                roomRepository.save(booking.getRoom());
            }

            System.out.println("🚫 [Scheduler] Hủy booking #" + booking.getId()
                    + " — quá 15 phút chưa thanh toán. Room → AVAILABLE. Không gửi notification.");
        }
    }
}