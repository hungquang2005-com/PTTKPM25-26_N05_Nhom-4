package com.hotel.hotelmanagement.config;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class BookingScheduler {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void autoReleaseExpiredRooms() {
        LocalDate today = LocalDate.now();
        System.out.println("⏰ [Scheduler] Đang kiểm tra phòng hết hạn... Today = " + today);

        // ✅ Dùng query trực tiếp từ DB thay vì filter stream trong Java
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(today);

        if (expiredBookings.isEmpty()) {
            System.out.println("⏰ [Scheduler] Không có phòng nào cần giải phóng.");
            return;
        }

        for (Booking booking : expiredBookings) {
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);

            if (booking.getRoom() != null) {
                // ✅ Kiểm tra có booking tương lai không trước khi về AVAILABLE
                List<Booking> futureBookings = bookingRepository.findConflictingBookings(
                        booking.getRoom().getId(),
                        today,
                        today.plusYears(1)
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
}