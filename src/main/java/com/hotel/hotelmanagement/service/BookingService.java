package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Customer;
import com.hotel.hotelmanagement.entity.Room;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.CustomerRepository;
import com.hotel.hotelmanagement.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý logic đặt phòng
 */
@Service
public class BookingService {
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    // Lấy tất cả booking
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
    
    // Lấy booking theo ID
    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }
    
    /**
     * Tạo đơn đặt phòng mới
     * @Transactional: đảm bảo toàn bộ thao tác thành công hoặc rollback
     */
    @Transactional
    public Booking createBooking(Long roomId, Customer customer, 
                                  LocalDate checkIn, LocalDate checkOut,
                                  String paymentMethod, String notes) {
        
        // 1. Kiểm tra phòng có tồn tại không
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng!"));
        
        // 2. Kiểm tra phòng có trống không
        if (!"AVAILABLE".equals(room.getStatus())) {
            throw new RuntimeException("Phòng đã được đặt hoặc đang bảo trì!");
        }
        
        // 3. Kiểm tra xung đột thời gian
        List<Booking> conflicts = bookingRepository
                .findConflictingBookings(roomId, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Phòng đã có người đặt trong khoảng thời gian này!");
        }
        
        // 4. Lưu thông tin khách hàng
        Customer savedCustomer = customerRepository.save(customer);
        
        // 5. Tính số đêm và tổng tiền
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new RuntimeException("Ngày trả phòng phải sau ngày nhận phòng!");
        }
        BigDecimal totalPrice = room.getPrice().multiply(BigDecimal.valueOf(nights));
        
        // 6. Tạo booking
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setCustomer(savedCustomer);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setTotalPrice(totalPrice);
        booking.setStatus("CONFIRMED");
        booking.setPaymentMethod(paymentMethod);
        booking.setPaymentStatus("UNPAID");
        booking.setNotes(notes);
        
        // 7. Cập nhật trạng thái phòng
        room.setStatus("BOOKED");
        roomRepository.save(room);
        
        return bookingRepository.save(booking);
    }
    
    // Hủy booking
    @Transactional
    public void cancelBooking(Long bookingId) {
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            booking.setStatus("CANCELLED");
            // Trả phòng về trạng thái trống
            booking.getRoom().setStatus("AVAILABLE");
            roomRepository.save(booking.getRoom());
            bookingRepository.save(booking);
        });
    }
    
    // Thanh toán
    @Transactional
    public void processPayment(Long bookingId, String paymentMethod) {
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            booking.setPaymentStatus("PAID");
            booking.setPaymentMethod(paymentMethod);
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);
        });
    }
}