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

/**
 * Scheduler tự động chạy mỗi ngày lúc 00:01
 *
 * FIX: Dùng LocalDate.now() thay vì yesterday.
 *
 * Ví dụ đặt 1 đêm: checkIn=24/05, checkOut=25/05
 *   Scheduler chạy 00:01 ngày 25/05:
 *     today = 25/05 → checkOut(25/05) <= 25/05 → TRUE → giải phóng ✅
 *
 * Khách trả phòng đúng ngày checkOut, scheduler chạy sau nửa đêm → hợp lý.
 */
@Component
public class BookingScheduler {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void autoReleaseExpiredRooms() {
        // FIX: dùng today thay vì yesterday
        // checkOut <= today nghĩa là hôm nay là ngày trả phòng hoặc đã qua
        LocalDate today = LocalDate.now();

        List<Booking> expiredBookings = bookingRepository
                .findByStatusIn(List.of("PENDING", "CONFIRMED"))
                .stream()
                .filter(b -> !b.getCheckOutDate().isAfter(today)) // checkOutDate <= today
                .toList();

        if (expiredBookings.isEmpty()) {
            System.out.println("⏰ [Scheduler] Không có phòng nào cần giải phóng.");
            return;
        }

        for (Booking booking : expiredBookings) {
            // FIX: Khóa cứng totalPrice tại thời điểm hoàn thành
            // totalPrice đã được set khi tạo booking và không thay đổi
            // → khi status = COMPLETED, dashboard chỉ đọc totalPrice đã lưu
            // → hủy sau này không ảnh hưởng vì CANCELLED không được tính vào doanh thu
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);

            if (booking.getRoom() != null) {
                booking.getRoom().setStatus("AVAILABLE");
                roomRepository.save(booking.getRoom());
                System.out.println("✅ [Scheduler] Giải phóng phòng: "
                        + booking.getRoom().getRoomNumber()
                        + " | Booking #" + booking.getId()
                        + " | CheckOut: " + booking.getCheckOutDate());
            }
        }

        System.out.println("✅ [Scheduler] Tổng đã giải phóng: " + expiredBookings.size() + " phòng.");
    }
}