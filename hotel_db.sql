-- ===================================
-- TẠO DATABASE
-- ===================================
CREATE DATABASE IF NOT EXISTS hotel_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE hotel_db;

-- ===================================
-- BẢNG PHÒNG
-- ===================================
CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(10) NOT NULL UNIQUE,
    room_type VARCHAR(50) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    capacity INT NOT NULL DEFAULT 2,
    description TEXT,
    image_url VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    floor INT DEFAULT 1,
    area DOUBLE DEFAULT 30.0,
    wifi BOOLEAN DEFAULT TRUE,
    breakfast BOOLEAN DEFAULT FALSE,
    air_conditioner BOOLEAN DEFAULT TRUE
);

-- ===================================
-- BẢNG KHÁCH HÀNG
-- ===================================
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    id_card VARCHAR(20),
    nationality VARCHAR(50) DEFAULT 'Việt Nam'
);

-- ===================================
-- BẢNG ĐẶT PHÒNG
-- ===================================
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    payment_status VARCHAR(20) DEFAULT 'UNPAID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- ===================================
-- BẢNG NGƯỜI DÙNG
-- ===================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    enabled BOOLEAN DEFAULT TRUE
);

-- ===================================
-- DỮ LIỆU MẪU - PHÒNG
-- ===================================
INSERT INTO rooms VALUES
(1, '101', 'Standard', 500000, 2, 'Phòng tiêu chuẩn thoáng mát, view nhìn ra sân vườn. Trang bị đầy đủ tiện nghi cơ bản.', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800', 'AVAILABLE', 1, 28, TRUE, FALSE, TRUE),
(2, '102', 'Standard', 500000, 2, 'Phòng tiêu chuẩn yên tĩnh, thiết kế tối giản hiện đại.', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800', 'AVAILABLE', 1, 28, TRUE, FALSE, TRUE),
(3, '201', 'Deluxe', 900000, 2, 'Phòng Deluxe sang trọng với ban công riêng, view thành phố tuyệt đẹp. Bữa sáng miễn phí.', 'https://images.unsplash.com/photo-1591088398332-8a7791972843?w=800', 'AVAILABLE', 2, 40, TRUE, TRUE, TRUE),
(4, '202', 'Deluxe', 950000, 3, 'Phòng Deluxe rộng rãi cho gia đình, nội thất cao cấp.', 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800', 'AVAILABLE', 2, 45, TRUE, TRUE, TRUE),
(5, '301', 'Suite', 2000000, 4, 'Suite cao cấp với phòng khách riêng, bồn tắm Jacuzzi, tầm nhìn panorama 180 độ.', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658?w=800', 'AVAILABLE', 3, 80, TRUE, TRUE, TRUE),
(6, '302', 'Suite', 2500000, 4, 'Presidential Suite - Trải nghiệm đỉnh cao của sự xa hoa. Butler service 24/7.', 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=800', 'AVAILABLE', 3, 100, TRUE, TRUE, TRUE),
(7, '401', 'Deluxe', 1200000, 2, 'Phòng Deluxe Premium tầng cao, thiết kế theo phong cách Đông Dương.', 'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800', 'AVAILABLE', 4, 50, TRUE, TRUE, TRUE),
(8, '103', 'Standard', 450000, 1, 'Phòng đơn tiết kiệm, phù hợp khách công tác.', 'https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?w=800', 'AVAILABLE', 1, 22, TRUE, FALSE, TRUE);

-- ===================================
-- TÀI KHOẢN ADMIN MẪU
-- Password: admin@123 (đã mã hóa BCrypt)
-- ===================================
INSERT INTO users (username, password, email, full_name, role) VALUES
('admin', '$2a$12$zQKevt.nSiQ8Z03QD198c.bNoNWZ.fV1MRn3tDJXn3HIevhitCoWy', 
 'admin@hotel.com', 'Quản trị viên', 'ROLE_ADMIN');

-- Thêm vào database nếu chưa có tài khoản admin
-- Password: admin123 (BCrypt)
INSERT IGNORE INTO users (username, password, email, full_name, role, enabled)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'admin@hotel.com',
    'Quản trị viên',
    'ROLE_ADMIN',
    true
);