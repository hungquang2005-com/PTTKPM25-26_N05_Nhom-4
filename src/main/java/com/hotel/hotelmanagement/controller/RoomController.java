package com.hotel.hotelmanagement.controller;

import com.hotel.hotelmanagement.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    /**
     * Fix: dùng @RequestParam(required=false, defaultValue="")
     * để tránh lỗi khi URL là /rooms?type=Suite
     */
    @GetMapping
    public String rooms(
            @RequestParam(required = false, defaultValue = "") String type,
            Model model) {

        if (!type.isBlank()) {
            model.addAttribute("rooms", roomService.getRoomsByType(type));
            model.addAttribute("selectedType", type);
        } else {
            model.addAttribute("rooms", roomService.getAllRooms());
            model.addAttribute("selectedType", "");
        }
        return "rooms";
    }

    @GetMapping("/{id}")
    public String roomDetail(@PathVariable Long id, Model model) {
        var room = roomService.getRoomById(id);
        if (room.isEmpty()) return "redirect:/rooms";
        model.addAttribute("room", room.get());
        model.addAttribute("relatedRooms",
            roomService.getRoomsByType(room.get().getRoomType())
                .stream().filter(r -> !r.getId().equals(id)).limit(3).toList());
        return "room-detail";
    }
}