package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Notification;
import com.hotel.hotelmanagement.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Notificationservice {

    @Autowired
    private NotificationRepository notificationRepository;

    // Gọi khi booking thành công (từ BookingController)
    public void createBookingNotification(Booking booking) {
        Notification noti = new Notification();
        noti.setBooking(booking);
        noti.setMessage("Khách " + booking.getCustomer().getFullName()
                + " vừa đặt phòng " + booking.getRoom().getRoomNumber()
                + " (" + booking.getRoom().getRoomType() + ")"
                + " từ " + booking.getCheckInDate()
                + " đến " + booking.getCheckOutDate());
        notificationRepository.save(noti);
    }

    // Lấy danh sách chưa đọc cho dropdown chuông
    public List<Notification> getUnreadNotifications() {
 
        return notificationRepository.findUnreadOrderByCreatedAtDesc();
    }

    public List<Notification> getRecentNotifications() {
        return notificationRepository.findTop20ByOrderByCreatedAtDesc();
    }

    // Đếm số badge đỏ
    public long countUnread() {
        // ✅ FIX: dùng method đổi tên mới thay vì countByIsReadFalse
        return notificationRepository.countUnread();
    }

    // Đánh dấu 1 thông báo đã đọc
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    // Đánh dấu tất cả đã đọc
    public void markAllAsRead() {
        List<Notification> unread = notificationRepository.findUnreadOrderByCreatedAtDesc();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}