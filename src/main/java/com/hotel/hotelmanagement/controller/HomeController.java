package com.hotel.hotelmanagement.controller;

import com.hotel.hotelmanagement.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private RoomService roomService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredRooms",
                roomService.getAvailableRooms().stream().limit(6).toList());
        return "home";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }
}