package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Tìm khách hàng theo email
    Optional<Customer> findByEmail(String email);
    
    // Tìm theo số điện thoại
    Optional<Customer> findByPhone(String phone);
}