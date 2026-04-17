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
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    // Trang đăng ký
    @GetMapping("/register")
    public String registerPage() {
        return "register";
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
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
}