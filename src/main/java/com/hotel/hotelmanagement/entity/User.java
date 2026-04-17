package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Entity đại diện cho tài khoản người dùng
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;    // Tên đăng nhập
    
    @Column(nullable = false)
    private String password;    // Mật khẩu (đã mã hóa BCrypt)
    
    @Column(nullable = false)
    private String email;       // Email
    
    @Column(name = "full_name")
    private String fullName;    // Họ tên
    
    @Column(nullable = false)
    private String role;        // ROLE_USER, ROLE_ADMIN
    
    @Column
    private boolean enabled = true; // Tài khoản còn hoạt động không
}
