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
import java.time.LocalDateTime;
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

        if (checkOut == null || !checkOut.isAfter(checkIn)) {
            checkOut = checkIn.plusDays(1);
        }

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
        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);
    }

    /**
     * Hủy booking theo 3 loại do admin chọn:
     *
     * EARLY_CANCEL (TH4): Hủy trong 30 phút
     *   → status = CANCELLED → không tính doanh thu
     *
     * LATE_CANCEL (TH1): Hủy sau 30 phút
     *   → totalPrice = 50% gốc, status = COMPLETED → khóa cứng doanh thu
     *
     * EARLY_CHECKOUT (TH2): Khách về sớm
     *   → totalPrice giữ 100%, status = COMPLETED → khóa cứng doanh thu
     *
     * TH3 (hết ngày): Scheduler tự xử lý, nút Hủy ẩn trên UI
     */
    @Transactional
    public void cancelBooking(Long bookingId, String cancelType) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking #" + bookingId));

        LocalDate today = LocalDate.now();

        // TH3: đã qua checkOutDate → không cho hủy tay
        if (!booking.getCheckOutDate().isAfter(today)) {
            throw new RuntimeException(
                "Booking #" + bookingId + " đã quá ngày trả phòng (" +
                booking.getCheckOutDate() + "). Doanh thu đã được khóa cứng bởi hệ thống."
            );
        }

        Room room = booking.getRoom();

        switch (cancelType) {

            case "EARLY_CANCEL" -> {
                // Validate phải trong 30 phút
                if (booking.getCreatedAt() != null) {
                    long minutesSinceCreated = ChronoUnit.MINUTES.between(
                        booking.getCreatedAt(), LocalDateTime.now()
                    );
                    if (minutesSinceCreated > 30) {
                        throw new RuntimeException(
                            "Booking #" + bookingId + " đã qua 30 phút kể từ lúc đặt (" +
                            minutesSinceCreated + " phút). Vui lòng chọn \"Hủy muộn\"."
                        );
                    }
                }
                // Hủy miễn phí — CANCELLED không tính doanh thu
                booking.setStatus("CANCELLED");
            }

            case "LATE_CANCEL" -> {
                // Phạt 50% — COMPLETED để khóa cứng vào doanh thu
                BigDecimal penalty = booking.getTotalPrice()
                        .multiply(BigDecimal.valueOf(0.5));
                booking.setTotalPrice(penalty);
                booking.setStatus("COMPLETED");
                booking.setPaymentStatus("PAID");
                booking.setNotes(
                    (booking.getNotes() != null ? booking.getNotes() + " | " : "") +
                    "Hủy muộn — phạt 50% = " + penalty + "đ"
                );
            }

            case "EARLY_CHECKOUT" -> {
                // Về sớm — giữ 100%, COMPLETED để khóa cứng doanh thu
                booking.setStatus("COMPLETED");
                booking.setPaymentStatus("PAID");
                booking.setNotes(
                    (booking.getNotes() != null ? booking.getNotes() + " | " : "") +
                    "Khách về sớm — giữ 100% = " + booking.getTotalPrice() + "đ"
                );
            }

            default -> throw new RuntimeException("Loại hủy không hợp lệ: " + cancelType);
        }

        // Trả phòng về AVAILABLE với mọi loại hủy
        if (room != null) {
            room.setStatus("AVAILABLE");
            roomRepository.save(room);
        }

        bookingRepository.save(booking);
    }
}