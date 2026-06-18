package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;  // ← THÊM DÒNG NÀY

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    List<Room> findByStatus(String status);
    List<Room> findByRoomType(String roomType);
    List<Room> findByStatusAndRoomType(String status, String roomType);
    List<Room> findByCapacityGreaterThanEqual(int capacity);

    Optional<Room> findByRoomNumber(String roomNumber);  // ← THÊM DÒNG NÀY

    @Query("SELECT r FROM Room r WHERE r.price BETWEEN :minPrice AND :maxPrice")
    List<Room> findByPriceRange(double minPrice, double maxPrice);
}