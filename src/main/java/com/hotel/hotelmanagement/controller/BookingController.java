package com.hotel.hotelmanagement.controller;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Customer;
import com.hotel.hotelmanagement.service.BookingService;
import com.hotel.hotelmanagement.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    // Trang đặt phòng
    @GetMapping("/booking/{roomId}")
    public String bookingForm(@PathVariable Long roomId, Model model) {
        var room = roomService.getRoomById(roomId);
        if (room.isEmpty()) {
            return "redirect:/rooms";
        }
        model.addAttribute("room", room.get());
        return "booking";
    }

    // Xử lý form đặt phòng
    @PostMapping("/booking")
    public String processBooking(
            @RequestParam Long roomId,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String idCard,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "CASH") String paymentMethod,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        try {
            Customer customer = new Customer();
            customer.setFullName(fullName);
            customer.setEmail(email);
            customer.setPhone(phone);
            customer.setIdCard(idCard);
            customer.setNationality("Việt Nam");

            Booking booking = bookingService.createBooking(
                    roomId, customer, checkIn, checkOut, paymentMethod, notes);

            return "redirect:/payment/" + booking.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/booking/" + roomId;
        }
    }

    // Trang thanh toán
    @GetMapping("/payment/{bookingId}")
    public String paymentPage(@PathVariable Long bookingId, Model model) {
        var booking = bookingService.getBookingById(bookingId);
        if (booking.isEmpty()) {
            return "redirect:/rooms";
        }
        model.addAttribute("booking", booking.get());
        return "payment";
    }

    // Xử lý thanh toán
    @PostMapping("/payment/{bookingId}")
    public String processPayment(
            @PathVariable Long bookingId,
            @RequestParam String paymentMethod,
            RedirectAttributes redirectAttributes) {

        try {
            bookingService.processPayment(bookingId, paymentMethod);
            redirectAttributes.addFlashAttribute("success",
                    "🎉 Thanh toán thành công! Cảm ơn quý khách.");
            return "redirect:/invoice/" + bookingId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/payment/" + bookingId;
        }
    }

    // Trang hóa đơn
    @GetMapping("/invoice/{bookingId}")
    public String invoicePage(@PathVariable Long bookingId, Model model) {
        var booking = bookingService.getBookingById(bookingId);
        if (booking.isEmpty()) {
            return "redirect:/rooms";
        }
        model.addAttribute("booking", booking.get());
        return "invoice";
    }
}