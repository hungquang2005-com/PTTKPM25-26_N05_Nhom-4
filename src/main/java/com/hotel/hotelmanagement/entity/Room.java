package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * Entity đại diện cho bảng phòng khách sạn
 */
@Entity
@Table(name = "rooms")
@Data               // Lombok: tự tạo getter, setter, toString
@NoArgsConstructor  // Lombok: tạo constructor không tham số
@AllArgsConstructor // Lombok: tạo constructor đầy đủ tham số
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_number", nullable = false, unique = true)
    private String roomNumber;  // Số phòng: 101, 102...
    
    @Column(name = "room_type", nullable = false)
    private String roomType;    // Loại: Standard, Deluxe, Suite
    
    @Column(nullable = false)
    private BigDecimal price;   // Giá mỗi đêm
    
    @Column(nullable = false)
    private int capacity;       // Sức chứa (số người)
    
    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả phòng
    
    @Column(name = "image_url")
    private String imageUrl;    // Đường dẫn ảnh
    
    @Column(nullable = false)
    private String status;      // AVAILABLE, BOOKED, MAINTENANCE
    
    @Column
    private int floor;          // Tầng
    
    @Column
    private double area;        // Diện tích (m²)
    
    @Column
    private boolean wifi;       // Có wifi không
    
    @Column
    private boolean breakfast;  // Có bữa sáng không
    
    @Column
    private boolean airConditioner; // Có điều hòa không
}