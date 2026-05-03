package com.hotel.hotelmanagement.controller;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Customer;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.CustomerRepository;
import com.hotel.hotelmanagement.service.BookingService;
import com.hotel.hotelmanagement.service.RoomService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class BookingController {

    @Autowired private BookingService bookingService;
    @Autowired private RoomService roomService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private BookingRepository bookingRepository;

    // ================= BOOKING =================
    @GetMapping("/booking/{roomId}")
    public String bookingForm(@PathVariable Long roomId, Model model) {
        var room = roomService.getRoomById(roomId);
        if (room.isEmpty()) return "redirect:/rooms";
        model.addAttribute("room", room.get());
        return "user/booking";
    }

    @PostMapping("/booking")
    public String processBooking(
            @RequestParam Long roomId,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String idCard,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false, defaultValue = "CASH") String paymentMethod,
            @RequestParam(required = false) String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            Customer customer = new Customer();
            customer.setFullName(fullName);
            customer.setEmail(email);
            customer.setPhone(phone);
            customer.setIdCard(idCard);

            // ✅ Lưu username vào customer để sau lọc my-bookings
            if (authentication != null) {
                customer.setUsername(authentication.getName());
            }

            customer = customerRepository.save(customer);

            Booking booking = bookingService.createBooking(
                    roomId, customer, checkIn, checkOut, paymentMethod, notes
            );

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
            RedirectAttributes redirectAttributes) {

        try {
            bookingService.processPayment(bookingId, paymentMethod);
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

        // ✅ Lấy username của user đang đăng nhập
        String username = authentication.getName();

        // ✅ Chỉ lấy booking của user này, mới nhất (ID lớn nhất) lên đầu
        List<Booking> bookings = bookingRepository
                .findByCustomerUsernameOrderByIdDesc(username);

        model.addAttribute("bookings", bookings);
        model.addAttribute("count", bookings.size());

        return "user/my-bookings";
    }
}