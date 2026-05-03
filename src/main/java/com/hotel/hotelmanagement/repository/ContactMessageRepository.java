package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    // Lấy tất cả tin nhắn, mới nhất lên đầu
    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    // Đếm tin nhắn chưa đọc (cho badge thông báo)
    long countByIsReadFalse();
}