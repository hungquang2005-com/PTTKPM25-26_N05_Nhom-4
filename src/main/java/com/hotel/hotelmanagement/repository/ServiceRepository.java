package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository xử lý truy vấn database cho Service (dịch vụ thêm)
 */
@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    // Lấy các dịch vụ đang được kích hoạt (hiển thị cho user chọn)
    List<Service> findByActiveTrue();
}
