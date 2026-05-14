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

@Service
public class BookingService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private CustomerRepository customerRepository;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    @Transactional
    public Booking createBooking(Long roomId, Customer customer,
                                 LocalDate checkIn, LocalDate checkOut,
                                 String paymentMethod, String notes) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại"));

        if (customer == null) {
            throw new RuntimeException("Customer không được null");
        }

        // FIX: Chuẩn hóa booking 1 ngày.
        // Nếu khách chọn cùng ngày (checkIn == checkOut) hoặc checkOut trước checkIn
        // → tự động set checkOut = checkIn + 1 ngày (tức ở 1 đêm).
        // Điều này đảm bảo:
        //   1. Scheduler biết đúng ngày cần giải phóng phòng (checkOut <= hôm qua)
        //   2. Query conflict half-open interval hoạt động đúng (checkIn < checkOut)
        if (checkOut == null || !checkOut.isAfter(checkIn)) {
            checkOut = checkIn.plusDays(1);
        }

        // Kiểm tra xung đột lịch (phòng đã được đặt trong khoảng này chưa)
        List<Booking> conflicts = bookingRepository.findConflictingBookings(roomId, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Phòng đã được đặt trong khoảng thời gian này!");
        }

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setCustomer(customer);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
        booking.setNotes(notes);
        booking.setStatus("PENDING");
        booking.setPaymentStatus("UNPAID");

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        // Sau khi chuẩn hóa ở trên, nights luôn >= 1
        BigDecimal totalPrice = room.getPrice().multiply(BigDecimal.valueOf(nights));
        booking.setTotalPrice(totalPrice);

        room.setStatus("BOOKED");
        roomRepository.save(room);

        return bookingRepository.save(booking);
    }

    @Transactional
    public void processPayment(Long bookingId, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        booking.setPaymentStatus("PAID");
        booking.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
        booking.setStatus("CONFIRMED"); // PAID → CONFIRMED (đang giữ phòng), Scheduler sẽ → COMPLETED sau checkOut
        bookingRepository.save(booking);
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        booking.setStatus("CANCELLED");

        Room room = booking.getRoom();
        room.setStatus("AVAILABLE");

        roomRepository.save(room);
        bookingRepository.save(booking);
    }
}