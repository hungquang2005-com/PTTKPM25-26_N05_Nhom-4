package com.hotel.hotelmanagement.controller;

import com.hotel.hotelmanagement.entity.ContactMessage;
import com.hotel.hotelmanagement.repository.ContactMessageRepository;
import com.hotel.hotelmanagement.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredRooms",
                roomService.getAvailableRooms().stream().limit(6).toList());
        return "user/home";
    }

    @GetMapping("/about")
    public String about() {
        return "user/about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "user/contact";
    }

    // ✅ Xử lý form liên hệ — lưu vào DB
    @PostMapping("/contact")
    public String submitContact(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String content,
            RedirectAttributes redirectAttributes) {

        try {
            ContactMessage message = new ContactMessage();
            message.setFullName(fullName);
            message.setEmail(email);
            message.setSubject(subject);
            message.setContent(content);
            message.setRead(false);

            contactMessageRepository.save(message);

            redirectAttributes.addFlashAttribute("success",
                    "✓ Tin nhắn của bạn đã được gửi thành công! Chúng tôi sẽ phản hồi sớm nhất.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Có lỗi xảy ra, vui lòng thử lại.");
        }

        return "redirect:/contact";
    }
}