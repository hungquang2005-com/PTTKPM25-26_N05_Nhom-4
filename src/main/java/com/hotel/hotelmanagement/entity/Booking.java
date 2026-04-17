package com.hotel.hotelmanagement.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho đơn đặt phòng
 */
@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Quan hệ nhiều-một với Room (nhiều booking cùng 1 phòng theo thời gian)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    
    // Quan hệ nhiều-một với Customer
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;   // Ngày nhận phòng
    
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;  // Ngày trả phòng
    
    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;   // Tổng tiền
    
    @Column(nullable = false)
    private String status;           // PENDING, CONFIRMED, CANCELLED, COMPLETED
    
    @Column(name = "payment_method")
    private String paymentMethod;    // CASH, CARD, TRANSFER
    
    @Column(name = "payment_status")
    private String paymentStatus;    // UNPAID, PAID
    
    @Column(name = "created_at")
    private LocalDateTime createdAt; // Thời gian tạo đơn
    
    @Column
    private String notes;            // Ghi chú thêm
    
    // Tự động set thời gian khi tạo
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}