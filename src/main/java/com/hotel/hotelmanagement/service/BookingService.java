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

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                roomId, checkIn, checkOut);
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

        // ✅ FIX: KHÔNG set room = BOOKED ở đây nữa.
        // Booking mới chỉ là PENDING/UNPAID -> phòng vẫn phải hiện TRỐNG cho khách khác.
        // Phòng chỉ chuyển BOOKED khi thanh toán thành công (xem processPayment()).

        return bookingRepository.save(booking);
    }

    // ✅ MỚI: cập nhật lại 1 booking PENDING đã có (dùng khi khách bấm "Quay lại" để
    // xem/sửa thông tin trước khi thanh toán, rồi submit lại). KHÔNG tạo booking mới,
    // nên không bao giờ tự đụng (conflict) với chính nó.
    @Transactional
    public Booking updateBooking(Long bookingId, Long roomId, Customer customerInfo,
                                  LocalDate checkIn, LocalDate checkOut,
                                  String paymentMethod, String notes) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking #" + bookingId));

        if (!"PENDING".equals(booking.getStatus()) || !"UNPAID".equals(booking.getPaymentStatus())) {
            throw new RuntimeException(
                "Booking #" + bookingId + " không còn ở trạng thái chờ thanh toán. Vui lòng đặt phòng lại.");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại"));

        if (checkOut == null || !checkOut.isAfter(checkIn)) {
            checkOut = checkIn.plusDays(1);
        }

        // Loại trừ chính booking này khi kiểm tra trùng lịch
        List<Booking> conflicts = bookingRepository.findConflictingBookingsExcluding(
                roomId, checkIn, checkOut, bookingId);
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Phòng đã được đặt trong khoảng thời gian này!");
        }

        // Cập nhật lại thông tin khách hàng đã gắn với booking này (không tạo customer mới)
        Customer customer = booking.getCustomer();
        customer.setFullName(customerInfo.getFullName());
        customer.setEmail(customerInfo.getEmail());
        customer.setPhone(customerInfo.getPhone());
        customer.setIdCard(customerInfo.getIdCard());
        if (customerInfo.getUsername() != null) {
            customer.setUsername(customerInfo.getUsername());
        }
        customerRepository.save(customer);

        booking.setRoom(room);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
        booking.setNotes(notes);

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        booking.setTotalPrice(room.getPrice().multiply(BigDecimal.valueOf(nights)));

        return bookingRepository.save(booking);
    }

    @Transactional
    public void processPayment(Long bookingId, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        // ✅ Guard: không cho thanh toán booking đã bị hủy
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new RuntimeException(
                "Booking #" + bookingId + " đã bị hủy, không thể thanh toán. " +
                "Vui lòng đặt phòng mới."
            );
        }

        // ✅ Guard: không cho thanh toán lại booking đã PAID
        if ("PAID".equals(booking.getPaymentStatus())) {
            throw new RuntimeException(
                "Booking #" + bookingId + " đã được thanh toán trước đó."
            );
        }

        booking.setPaymentStatus("PAID");
        booking.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);

        // ✅ FIX: phòng chỉ chuyển sang "hết phòng" (BOOKED) tại đây — khi thanh toán THÀNH CÔNG.
        Room room = booking.getRoom();
        if (room != null) {
            room.setStatus("BOOKED");
            roomRepository.save(room);
        }
    }

    @Transactional
    public void cancelBooking(Long bookingId, String cancelType) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking #" + bookingId));

        LocalDate today = LocalDate.now();

        if (!booking.getCheckOutDate().isAfter(today)) {
            throw new RuntimeException(
                "Booking #" + bookingId + " đã quá ngày trả phòng (" +
                booking.getCheckOutDate() + "). Doanh thu đã được khóa cứng bởi hệ thống."
            );
        }

        Room room = booking.getRoom();

        switch (cancelType) {

            case "EARLY_CANCEL" -> {
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
                booking.setStatus("CANCELLED");
            }

            case "LATE_CANCEL" -> {
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
                booking.setStatus("COMPLETED");
                booking.setPaymentStatus("PAID");
                booking.setNotes(
                    (booking.getNotes() != null ? booking.getNotes() + " | " : "") +
                    "Khách về sớm — giữ 100% = " + booking.getTotalPrice() + "đ"
                );
            }

            default -> throw new RuntimeException("Loại hủy không hợp lệ: " + cancelType);
        }

        if (room != null) {
            room.setStatus("AVAILABLE");
            roomRepository.save(room);
        }

        bookingRepository.save(booking);
    }
}