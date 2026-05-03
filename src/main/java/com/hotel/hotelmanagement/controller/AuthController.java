package com.hotel.hotelmanagement.controller;

import com.hotel.hotelmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller xử lý đăng ký và đăng nhập
 */
@Controller
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    // Trang đăng nhập
    // Giữ nguyên URL là /login cho đẹp (trên trình duyệt sẽ là localhost:8080/login)
    @GetMapping("/login") 
    public String loginPage() {
        // Nhưng trả về giao diện nằm trong thư mục user
        return "user/login"; 
    }
    
    // Trang đăng ký
    @GetMapping("/register")
    public String registerPage() {
        return "user/register";
    }
    
    // Xử lý đăng ký
    @PostMapping("/register")
    public String processRegister(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String fullName,
            RedirectAttributes redirectAttributes) {
        
        try {
            userService.registerUser(username, email, password, fullName);
            redirectAttributes.addFlashAttribute("success", 
                    "Đăng ký thành công! Vui lòng đăng nhập.");
            // Redirect về đúng URL trang đăng nhập (có dấu / ở đầu)
            return "redirect:/login"; 
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            // Redirect về đúng URL trang đăng ký
            return "redirect:/register"; 
        }
    }
}