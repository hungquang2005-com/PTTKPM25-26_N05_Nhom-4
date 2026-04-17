package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Tìm user theo username
    Optional<User> findByUsername(String username);
    
    // Tìm user theo email
    Optional<User> findByEmail(String email);
    
    // Kiểm tra username đã tồn tại chưa
    boolean existsByUsername(String username);
    
    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);
}