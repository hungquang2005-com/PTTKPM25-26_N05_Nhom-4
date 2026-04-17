package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Entity đại diện cho khách hàng
 */
@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;    // Họ tên đầy đủ
    
    @Column(nullable = false)
    private String email;       // Email
    
    @Column
    private String phone;       // Số điện thoại
    
    @Column
    private String address;     // Địa chỉ
    
    @Column
    private String idCard;      // CCCD/CMND
    
    @Column
    private String nationality; // Quốc tịch
}