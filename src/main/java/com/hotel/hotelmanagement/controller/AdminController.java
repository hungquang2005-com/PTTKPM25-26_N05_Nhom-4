package com.hotel.hotelmanagement.controller;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.ContactMessage;
import com.hotel.hotelmanagement.entity.Room;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.ContactMessageRepository;
import com.hotel.hotelmanagement.repository.CustomerRepository;
import com.hotel.hotelmanagement.repository.RoomRepository;
import com.hotel.hotelmanagement.repository.UserRepository;
import com.hotel.hotelmanagement.service.BookingService;
import com.hotel.hotelmanagement.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin")
// KHÔNG đặt @PreAuthorize ở đây vì /admin/login cần public
public class AdminController {

    @Autowired private RoomRepository roomRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomService roomService;
    @Autowired private BookingService bookingService;
    @Autowired private ContactMessageRepository contactMessageRepository;

    // ============================================================
    // LOGIN – public, không cần xác thực
    // Spring Security tự bắt POST /admin/login, không cần viết handler POST
    // ============================================================
    @GetMapping("/login")
    public String loginPage() {
        return "admin/admin-login";  // templates/admin/admin-login.html
    }

    // ============================================================
    // DASHBOARD – Security đã bảo vệ bằng hasRole("ADMIN")
    // ============================================================
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalRooms",     roomRepository.count());
        model.addAttribute("availableRooms", roomRepository.findByStatus("AVAILABLE").size());
        model.addAttribute("totalBookings",  bookingRepository.count());
        model.addAttribute("totalCustomers", customerRepository.count());
        model.addAttribute("unreadMessages", contactMessageRepository.countByIsReadFalse());

        LocalDate now = LocalDate.now();
        BigDecimal monthRevenue = bookingRepository.findAll().stream()
            .filter(b -> "COMPLETED".equals(b.getStatus())
                      && b.getCreatedAt() != null
                      && b.getCreatedAt().getMonth() == now.getMonth()
                      && b.getCreatedAt().getYear() == now.getYear())
            .map(Booking::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("monthRevenue", monthRevenue);

        BigDecimal totalRevenue = bookingRepository.findAll().stream()
            .filter(b -> "COMPLETED".equals(b.getStatus()))
            .map(Booking::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalRevenue", totalRevenue);

        List<Booking> recentBookings = bookingRepository.findAll().stream()
            .sorted(Comparator.comparing(Booking::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(10)
            .toList();
        model.addAttribute("recentBookings", recentBookings);
        model.addAttribute("chartData", getRevenueChartData());

        return "admin/dashboard";
    }

    // ===== PHÒNG =====
    @GetMapping("/rooms")
    public String manageRooms(Model model) {
        model.addAttribute("rooms", roomRepository.findAll());
        model.addAttribute("newRoom", new Room());
        return "admin/rooms";
    }

    @PostMapping("/rooms/add")
    public String addRoom(@ModelAttribute Room room, RedirectAttributes ra) {
        room.setStatus("AVAILABLE");
        roomRepository.save(room);
        ra.addFlashAttribute("success", "Thêm phòng thành công!");
        return "redirect:/admin/rooms";
    }

    @GetMapping("/rooms/edit/{id}")
    public String editRoomForm(@PathVariable Long id, Model model) {
        roomRepository.findById(id).ifPresent(r -> model.addAttribute("room", r));
        return "admin/room-edit";
    }

    @PostMapping("/rooms/edit/{id}")
    public String editRoom(@PathVariable Long id, @ModelAttribute Room room, RedirectAttributes ra) {
        room.setId(id);
        roomRepository.save(room);
        ra.addFlashAttribute("success", "Cập nhật phòng thành công!");
        return "redirect:/admin/rooms";
    }

    @GetMapping("/rooms/delete/{id}")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes ra) {
        roomRepository.deleteById(id);
        ra.addFlashAttribute("success", "Đã xóa phòng!");
        return "redirect:/admin/rooms";
    }

    // ===== BOOKING =====
    @GetMapping("/bookings")
    public String manageBookings(Model model) {
        List<Booking> bookings = bookingRepository.findAll().stream()
            .sorted(Comparator.comparing(Booking::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
        model.addAttribute("bookings", bookings);
        return "admin/bookings";
    }

    @GetMapping("/bookings/cancel/{id}")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes ra) {
        bookingService.cancelBooking(id);
        ra.addFlashAttribute("success", "Đã hủy booking #" + id);
        return "redirect:/admin/bookings";
    }

    // ===== KHÁCH HÀNG =====
    @GetMapping("/customers")
    public String manageCustomers(Model model) {
        model.addAttribute("customers", customerRepository.findAll());
        return "admin/customers";
    }

    // ===== THỐNG KÊ =====
    @GetMapping("/stats")
    public String stats(Model model) {
        List<Booking> allCompleted = bookingRepository.findAll().stream()
            .filter(b -> "COMPLETED".equals(b.getStatus()))
            .toList();

        int year = LocalDate.now().getYear();
        Map<String, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
        String[] months = {"T1","T2","T3","T4","T5","T6","T7","T8","T9","T10","T11","T12"};
        for (int i = 0; i < 12; i++) {
            final int month = i + 1;
            BigDecimal rev = allCompleted.stream()
                .filter(b -> b.getCreatedAt() != null
                          && b.getCreatedAt().getYear() == year
                          && b.getCreatedAt().getMonthValue() == month)
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            monthlyRevenue.put(months[i], rev);
        }

        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("totalRevenue",
            allCompleted.stream().map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("totalBookings", allCompleted.size());
        model.addAttribute("currentYear", year);

        return "admin/stats";
    }

    // ===== TIN NHẮN =====
    @GetMapping("/messages")
    public String messages(Model model) {
        model.addAttribute("messages", contactMessageRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("unreadCount", contactMessageRepository.countByIsReadFalse());
        return "admin/messages";
    }

    @GetMapping("/messages/read/{id}")
    public String markAsRead(@PathVariable Long id) {
        contactMessageRepository.findById(id).ifPresent(m -> {
            m.setRead(true);
            contactMessageRepository.save(m);
        });
        return "redirect:/admin/messages";
    }

    @GetMapping("/messages/delete/{id}")
    public String deleteMessage(@PathVariable Long id, RedirectAttributes ra) {
        contactMessageRepository.deleteById(id);
        ra.addFlashAttribute("success", "Đã xóa tin nhắn!");
        return "redirect:/admin/messages";
    }

    // ===== HELPER =====
    private List<Map<String, Object>> getRevenueChartData() {
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDate now = LocalDate.now();
        String[] monthNames = {"T1","T2","T3","T4","T5","T6","T7","T8","T9","T10","T11","T12"};

        for (int i = 5; i >= 0; i--) {
            LocalDate d = now.minusMonths(i);
            final int m = d.getMonthValue(), y = d.getYear();
            BigDecimal rev = bookingRepository.findAll().stream()
                .filter(b -> "COMPLETED".equals(b.getStatus())
                          && b.getCreatedAt() != null
                          && b.getCreatedAt().getMonthValue() == m
                          && b.getCreatedAt().getYear() == y)
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> entry = new HashMap<>();
            entry.put("label", monthNames[m - 1] + "/" + y);
            entry.put("value", rev);
            data.add(entry);
        }
        return data;
    }
}