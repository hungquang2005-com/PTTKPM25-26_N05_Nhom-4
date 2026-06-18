package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.read = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadOrderByCreatedAtDesc();

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.read = false")
    long countUnread();

    List<Notification> findTop20ByOrderByCreatedAtDesc();
}