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
 * Kiểm tra phòng đã hết hạn đặt và trả về trạng thái AVAILABLE
 */
@Component
public class BookingScheduler {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * Chạy lúc 00:01 mỗi ngày
     * cron = "giây phút giờ ngày tháng thứ"
     *
     * FIX: Chỉ tìm booking đang active (PENDING / CONFIRMED) mà checkOutDate < hôm nay
     *      → set COMPLETED + trả phòng về AVAILABLE.
     *      Booking đã COMPLETED / CANCELLED không cần xử lý lại.
     */
    /**
     * Chạy lúc 00:01 mỗi ngày.
     *
     * Luồng trạng thái:
     *   PENDING → (thanh toán) → CONFIRMED → (hết checkOut) → COMPLETED
     *
     * Ví dụ đặt 1 ngày:
     *   checkIn = 14/05  |  checkOut = 15/05  (BookingService tự chuẩn hóa)
     *
     *   Scheduler chạy 00:01 ngày 15/05:
     *     yesterday = 14/05 → checkOut(15/05) <= 14/05 → FALSE → chưa giải phóng ✓
     *
     *   Scheduler chạy 00:01 ngày 16/05:
     *     yesterday = 15/05 → checkOut(15/05) <= 15/05 → TRUE  → giải phóng ✅
     */
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void autoReleaseExpiredRooms() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        List<Booking> expiredBookings = bookingRepository
                .findByStatusIn(List.of("PENDING", "CONFIRMED"))
                .stream()
                .filter(b -> !b.getCheckOutDate().isAfter(yesterday)) // checkOutDate <= hôm qua
                .toList();

        if (expiredBookings.isEmpty()) {
            System.out.println("⏰ [Scheduler] Không có phòng nào cần giải phóng.");
            return;
        }

        for (Booking booking : expiredBookings) {
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