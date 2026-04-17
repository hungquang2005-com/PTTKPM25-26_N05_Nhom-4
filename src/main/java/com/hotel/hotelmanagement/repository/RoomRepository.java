package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository xử lý truy vấn database cho Room
 * JpaRepository cung cấp sẵn các method: findAll, findById, save, delete...
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    // Tìm phòng theo trạng thái
    List<Room> findByStatus(String status);
    
    // Tìm phòng theo loại
    List<Room> findByRoomType(String roomType);
    
    // Tìm phòng trống theo loại
    List<Room> findByStatusAndRoomType(String status, String roomType);
    
    // Tìm phòng theo sức chứa >= yêu cầu
    List<Room> findByCapacityGreaterThanEqual(int capacity);
    
    // Query tìm phòng có giá trong khoảng
    @Query("SELECT r FROM Room r WHERE r.price BETWEEN :minPrice AND :maxPrice")
    List<Room> findByPriceRange(double minPrice, double maxPrice);
}