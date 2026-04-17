package com.hotel.hotelmanagement.config;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
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
@EnableScheduling
public class BookingScheduler {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * Chạy lúc 00:01 mỗi ngày
     * cron = "giây phút giờ ngày tháng thứ"
     */
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void autoReleaseExpiredRooms() {
        LocalDate today = LocalDate.now();

        // Tìm tất cả booking đã COMPLETED hoặc check-out date đã qua
        List<Booking> expiredBookings = bookingRepository
                .findByStatusIn(List.of("CONFIRMED", "COMPLETED"))
                .stream()
                .filter(b -> b.getCheckOutDate().isBefore(today) ||
                             b.getCheckOutDate().isEqual(today))
                .toList();

        for (Booking booking : expiredBookings) {
            // Cập nhật trạng thái booking
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);

            // Trả phòng về AVAILABLE
            booking.getRoom().setStatus("AVAILABLE");
            roomRepository.save(booking.getRoom());

            System.out.println("✅ Auto-released room: " + booking.getRoom().getRoomNumber()
                    + " (Booking #" + booking.getId() + ")");
        }
    }
}