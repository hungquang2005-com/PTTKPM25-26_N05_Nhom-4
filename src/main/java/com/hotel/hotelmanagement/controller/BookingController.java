package com.hotel.hotelmanagement.controller;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Customer;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.CustomerRepository;
import com.hotel.hotelmanagement.service.BookingService;
import com.hotel.hotelmanagement.service.Notificationservice;
import com.hotel.hotelmanagement.service.RoomService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Controller
public class BookingController {

    // ✅ Key lưu trong session: id của booking PENDING đang "dang dở" của khách
    // Dùng để nhận ra khi khách bấm Quay lại rồi gửi lại form -> SỬA booking cũ,
    // không tạo booking mới (tránh tự đụng lịch với chính mình).
    private static final String PENDING_BOOKING_SESSION_KEY = "pendingBookingId";

    @Autowired private BookingService bookingService;
    @Autowired private RoomService roomService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private Notificationservice notificationService;

    // ================= BOOKING =================
    @GetMapping("/booking/{roomId}")
    public String bookingForm(@PathVariable Long roomId, HttpSession session, Model model) {
        var room = roomService.getRoomById(roomId);
        if (room.isEmpty()) return "redirect:/rooms";
        model.addAttribute("room", room.get());

        // ✅ Nếu khách đang có 1 booking PENDING/UNPAID dang dở cho ĐÚNG phòng này
        // (vừa tạo ở lượt trước, giờ quay lại xem/sửa) -> đổ lại thông tin cũ lên form
        Booking existing = getReusablePendingBooking(session, roomId);
        if (existing != null) {
            model.addAttribute("existingBooking", existing);
            model.addAttribute("customer", existing.getCustomer());
        }

        return "user/booking";
    }

    /**
     * Trả về booking PENDING/UNPAID đang lưu trong session nếu nó còn hợp lệ
     * và thuộc đúng phòng roomId. Nếu không hợp lệ (đã thanh toán/hủy/hết hạn),
     * tự dọn session và trả về null.
     */
    private Booking getReusablePendingBooking(HttpSession session, Long roomId) {
        Object raw = session.getAttribute(PENDING_BOOKING_SESSION_KEY);
        if (!(raw instanceof Long pendingId)) return null;

        Optional<Booking> opt = bookingService.getBookingById(pendingId);
        if (opt.isPresent()) {
            Booking b = opt.get();
            boolean reusable = "PENDING".equals(b.getStatus())
                    && "UNPAID".equals(b.getPaymentStatus())
                    && b.getRoom() != null
                    && b.getRoom().getId().equals(roomId);
            if (reusable) return b;
        }

        // Booking cũ không còn dùng được nữa (đã hủy/đã thanh toán/khác phòng) -> dọn session
        session.removeAttribute(PENDING_BOOKING_SESSION_KEY);
        return null;
    }

    @PostMapping("/booking")
    public String processBooking(
            @RequestParam Long roomId,
            @RequestParam(required = false) Long bookingId,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String idCard,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false, defaultValue = "CASH") String paymentMethod,
            @RequestParam(required = false) String notes,
            Authentication authentication,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            Customer customerInfo = new Customer();
            customerInfo.setFullName(fullName);
            customerInfo.setEmail(email);
            customerInfo.setPhone(phone);
            customerInfo.setIdCard(idCard);
            if (authentication != null) {
                customerInfo.setUsername(authentication.getName());
            }

            // ✅ Xác định booking cần SỬA: ưu tiên bookingId gửi từ form (hidden field),
            // nếu form không có thì fallback kiểm tra trong session — bắt được cả trường hợp
            // khách dùng nút Back của trình duyệt (form cũ không có hidden field).
            Long targetBookingId = bookingId;
            if (targetBookingId == null) {
                Booking reusable = getReusablePendingBooking(session, roomId);
                if (reusable != null) {
                    targetBookingId = reusable.getId();
                }
            }

            Booking booking;
            if (targetBookingId != null) {
                // SỬA booking PENDING đã có -> không tạo mới, không tự đụng lịch với mình
                booking = bookingService.updateBooking(
                        targetBookingId, roomId, customerInfo, checkIn, checkOut, paymentMethod, notes);
            } else {
                Customer customer = customerRepository.save(customerInfo);
                booking = bookingService.createBooking(
                        roomId, customer, checkIn, checkOut, paymentMethod, notes
                );
            }

            session.setAttribute(PENDING_BOOKING_SESSION_KEY, booking.getId());

            // ❌ ĐÃ XÓA: notificationService.createBookingNotification(booking);
            // Lý do: booking lúc này còn PENDING/UNPAID, chưa thanh toán
            // Notification sẽ được gửi trong processPayment() sau khi PAID thành công

            return "redirect:/payment/" + booking.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi đặt phòng: " + e.getMessage());
            return "redirect:/booking/" + roomId;
        }
    }

    // ================= PAYMENT =================
    @GetMapping("/payment/{bookingId}")
    public String paymentPage(@PathVariable Long bookingId, Model model) {
        var opt = bookingService.getBookingById(bookingId);
        if (opt.isEmpty()) return "redirect:/rooms";

        Booking b = opt.get();

        // ✅ Nếu booking đã bị hủy (do Back quá lâu hoặc timeout), redirect về rooms
        if ("CANCELLED".equals(b.getStatus())) {
            model.addAttribute("error", "Booking #" + bookingId + " đã bị hủy.");
            return "redirect:/rooms";
        }

        long nights = ChronoUnit.DAYS.between(b.getCheckInDate(), b.getCheckOutDate());
        if (nights <= 0) nights = 1;

        model.addAttribute("booking", b);
        model.addAttribute("nights", nights);
        return "user/payment";
    }

    @PostMapping("/payment/{bookingId}")
    public String processPayment(
            @PathVariable Long bookingId,
            @RequestParam(required = false, defaultValue = "CASH") String paymentMethod,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            bookingService.processPayment(bookingId, paymentMethod);

            // ✅ NOTIFICATION CHỈ GỬI KHI THANH TOÁN THÀNH CÔNG
            Booking paidBooking = bookingService.getBookingById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));
            notificationService.createBookingNotification(paidBooking);

            // ✅ Thanh toán xong -> bỏ booking này khỏi session "đang dang dở"
            session.removeAttribute(PENDING_BOOKING_SESSION_KEY);

            redirectAttributes.addFlashAttribute("success", "🎉 Thanh toán thành công!");
            return "redirect:/invoice/" + bookingId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/payment/" + bookingId;
        }
    }

    // ================= INVOICE =================
    @GetMapping("/invoice/{bookingId}")
    public String invoicePage(@PathVariable Long bookingId, Model model) {
        var opt = bookingService.getBookingById(bookingId);
        if (opt.isEmpty()) return "redirect:/rooms";

        Booking b = opt.get();
        long nights = ChronoUnit.DAYS.between(b.getCheckInDate(), b.getCheckOutDate());
        if (nights <= 0) nights = 1;

        model.addAttribute("booking", b);
        model.addAttribute("nights", nights);
        return "user/invoice";
    }

    // ================= MY BOOKINGS =================
    @GetMapping("/my-bookings")
    public String myBookings(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        List<Booking> bookings = bookingRepository
                .findByCustomerUsernameOrderByIdDesc(username);

        model.addAttribute("bookings", bookings);
        model.addAttribute("count", bookings.size());
        return "user/my-bookings";
    }
}