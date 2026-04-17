package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.Room;
import com.hotel.hotelmanagement.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý logic nghiệp vụ cho phòng
 */
@Service
public class RoomService {
    
    @Autowired
    private RoomRepository roomRepository;
    
    // Lấy tất cả phòng
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }
    
    // Lấy phòng theo ID
    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }
    
    // Lấy tất cả phòng trống
    public List<Room> getAvailableRooms() {
        return roomRepository.findByStatus("AVAILABLE");
    }
    
    // Lấy phòng theo loại
    public List<Room> getRoomsByType(String roomType) {
        return roomRepository.findByRoomType(roomType);
    }
    
    // Lưu hoặc cập nhật phòng
    public Room saveRoom(Room room) {
        return roomRepository.save(room);
    }
    
    // Xóa phòng
    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }
    
    // Cập nhật trạng thái phòng
    public void updateRoomStatus(Long roomId, String status) {
        roomRepository.findById(roomId).ifPresent(room -> {
            room.setStatus(status);
            roomRepository.save(room);
        });
    }
}