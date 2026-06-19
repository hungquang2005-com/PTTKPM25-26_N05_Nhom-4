package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;        // Tên dịch vụ: "Massage thư giãn", "Xông hơi"...

    @Column(nullable = false)
    private BigDecimal price;   // Giá dịch vụ (tính 1 lần/cả kỳ ở)

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả ngắn

    @Column
    private String icon;        // Emoji/icon hiển thị, vd: 💆, 🧖, 🍽️

    @Column(nullable = false)
    private boolean active = true; // Admin có thể ẩn dịch vụ mà không cần xóa
}