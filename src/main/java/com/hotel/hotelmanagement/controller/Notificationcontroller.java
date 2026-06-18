package com.hotel.hotelmanagement.controller;

import com.hotel.hotelmanagement.entity.Notification;
import com.hotel.hotelmanagement.service.Notificationservice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/notifications")
public class Notificationcontroller {

    @Autowired
    private Notificationservice notificationService;

    // Trả về số badge đỏ trên chuông
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread()));
    }

    // Trả về danh sách thông báo GẦN ĐÂY cho dropdown (gồm cả đã đọc, kèm cờ "read"
    // để frontend hiển thị mờ/đậm khác nhau) -> bấm "Đọc hết" không bị mất lịch sử
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        List<Map<String, Object>> result = notificationService.getRecentNotifications()
                .stream()
                .map(n -> Map.<String, Object>of(
                        "id",        n.getId(),
                        "message",   n.getMessage(),
                        "createdAt", n.getCreatedAt() != null ? n.getCreatedAt().format(fmt) : "",
                        "bookingId", n.getBooking() != null ? n.getBooking().getId() : "",
                        "read",      n.isRead()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // Đánh dấu 1 thông báo đã đọc
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    // Đánh dấu tất cả đã đọc
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }
}