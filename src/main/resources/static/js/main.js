/**
 * HOTEL MANAGEMENT SYSTEM - MAIN JAVASCRIPT
 * Xử lý các hiệu ứng và tương tác giao diện
 */

// ===================================
// 1. NAVBAR SCROLL EFFECT
// ===================================
window.addEventListener('scroll', function() {
    const navbar = document.querySelector('.navbar');
    if (navbar) {
        if (window.scrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    }
});

// ===================================
// 2. MOBILE MENU TOGGLE
// ===================================
const navToggle = document.querySelector('.nav-toggle');
const navMenu = document.querySelector('.nav-menu');

if (navToggle && navMenu) {
    navToggle.addEventListener('click', function() {
        navMenu.classList.toggle('open');
        // Animate hamburger icon
        const spans = this.querySelectorAll('span');
        spans.forEach(span => span.classList.toggle('active'));
    });
    
    // Đóng menu khi click link
    navMenu.querySelectorAll('a').forEach(link => {
        link.addEventListener('click', () => {
            navMenu.classList.remove('open');
        });
    });
}

// ===================================
// 3. SCROLL-TRIGGERED ANIMATIONS
// ===================================
function initScrollReveal() {
    const elements = document.querySelectorAll('.reveal');
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                observer.unobserve(entry.target); // Chỉ animate 1 lần
            }
        });
    }, {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    });
    
    elements.forEach(el => observer.observe(el));
}

// ===================================
// 4. TÍNH TIỀN ĐẶT PHÒNG TỰ ĐỘNG
// ===================================
function initBookingCalculator() {
    const checkIn = document.getElementById('checkIn');
    const checkOut = document.getElementById('checkOut');
    const totalDisplay = document.getElementById('totalPrice');
    const nightsDisplay = document.getElementById('nightsCount');
    const pricePerNight = document.getElementById('pricePerNight');
    const roomSubtotalDisplay = document.getElementById('roomSubtotal');
    const serviceSubtotalDisplay = document.getElementById('serviceSubtotal');
    const serviceCheckboxes = document.querySelectorAll('.service-checkbox');

    if (!checkIn || !checkOut) return;

    // Set ngày tối thiểu là hôm nay
    const today = new Date().toISOString().split('T')[0];
    checkIn.min = today;

    function getServicesTotal() {
        let total = 0;
        serviceCheckboxes.forEach(cb => {
            if (cb.checked) {
                total += parseFloat(cb.dataset.price || 0);
            }
            // Highlight thẻ dịch vụ khi được chọn
            const wrapper = cb.closest('.service-option');
            if (wrapper) {
                wrapper.style.borderColor = cb.checked ? 'var(--accent)' : 'var(--border)';
                wrapper.style.background = cb.checked ? 'rgba(201,168,76,0.08)' : 'rgba(255,255,255,0.03)';
            }
        });
        return total;
    }

    function calculateTotal() {
        const inDate = new Date(checkIn.value);
        const outDate = new Date(checkOut.value);
        const servicesTotal = getServicesTotal();

        // Cập nhật tổng dịch vụ ở khối "Dịch vụ thêm" (nếu có)
        const servicesTotalBox = document.getElementById('servicesTotal');
        if (servicesTotalBox) servicesTotalBox.textContent = formatCurrency(servicesTotal);
        if (serviceSubtotalDisplay) serviceSubtotalDisplay.textContent = formatCurrency(servicesTotal);

        if (checkIn.value && checkOut.value && outDate > inDate) {
            const nights = Math.ceil((outDate - inDate) / (1000 * 60 * 60 * 24));
            const pricePerN = parseFloat(pricePerNight?.dataset.price || 0);
            const roomTotal = nights * pricePerN;
            const total = roomTotal + servicesTotal;

            if (nightsDisplay) nightsDisplay.textContent = nights + ' đêm';
            if (roomSubtotalDisplay) roomSubtotalDisplay.textContent = formatCurrency(roomTotal);
            if (totalDisplay) {
                totalDisplay.textContent = formatCurrency(total);
                // Animate số thay đổi
                totalDisplay.classList.add('price-updated');
                setTimeout(() => totalDisplay.classList.remove('price-updated'), 300);
            }
        } else {
            // Chưa chọn đủ ngày: vẫn hiển thị tổng dịch vụ nếu có chọn
            if (totalDisplay && servicesTotal > 0) {
                totalDisplay.textContent = formatCurrency(servicesTotal) + ' (+ tiền phòng)';
            }
        }
    }

    // Cập nhật ngày check-out tối thiểu khi chọn check-in
    checkIn.addEventListener('change', function() {
        const nextDay = new Date(this.value);
        nextDay.setDate(nextDay.getDate() + 1);
        checkOut.min = nextDay.toISOString().split('T')[0];

        if (checkOut.value && new Date(checkOut.value) <= new Date(this.value)) {
            checkOut.value = nextDay.toISOString().split('T')[0];
        }
        calculateTotal();
    });

    checkOut.addEventListener('change', calculateTotal);

    // Tích/bỏ tích dịch vụ -> tính lại tổng ngay
    serviceCheckboxes.forEach(cb => {
        cb.addEventListener('change', calculateTotal);
    });

    // Tính lần đầu khi load trang (để hiện tổng dịch vụ = 0 đ ngay)
    calculateTotal();
}

// ===================================
// 5. PAYMENT METHOD SELECTION (ĐÃ ĐƯỢC FIX LỖI KẸT QR)
// ===================================
function initPaymentMethods() {
    const payCards = document.querySelectorAll('.pay-card');
    const hiddenInput = document.getElementById('hPayMethod');
    
    // Nếu trang hiện tại có chứa form thanh toán thì mới chạy
    if (payCards.length > 0 && hiddenInput) {
        
        // 1. Gắn sự kiện Click cho từng thẻ chọn phương thức
        payCards.forEach(card => {
            card.addEventListener('click', function() {
                // Bỏ chọn tất cả
                payCards.forEach(c => c.classList.remove('active'));
                
                // Chọn thẻ được click
                this.classList.add('active');
                
                // Xác định giá trị dựa vào ID của thẻ
                let methodValue = 'CASH';
                if (this.id === 'qr') methodValue = 'TRANSFER';
                else if (this.id === 'card') methodValue = 'CARD';
                else if (this.id === 'cash') methodValue = 'CASH';
                
                // Gán vào input ẩn để gửi về Backend (sửa lỗi 500)
                hiddenInput.value = methodValue;
            });
        });

        // 2. Tự động chọn đúng phương thức lúc load trang lần đầu (nếu back lại từ trang lỗi)
        let currentMethod = hiddenInput.value;
        payCards.forEach(c => c.classList.remove('active')); // Reset
        
        if (currentMethod === 'TRANSFER' && document.getElementById('qr')) {
            document.getElementById('qr').classList.add('active');
        } else if (currentMethod === 'CARD' && document.getElementById('card')) {
            document.getElementById('card').classList.add('active');
        } else if (document.getElementById('cash')) {
            document.getElementById('cash').classList.add('active');
            hiddenInput.value = 'CASH'; // Đảm bảo luôn có giá trị mặc định
        }
    }
}

// ===================================
// 6. FORM VALIDATION
// ===================================
function initFormValidation() {
    const forms = document.querySelectorAll('form[data-validate]');
    
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            let isValid = true;
            const requiredFields = this.querySelectorAll('[required]');
            
            requiredFields.forEach(field => {
                if (!field.value.trim()) {
                    isValid = false;
                    showFieldError(field, 'Vui lòng điền thông tin này!');
                } else {
                    clearFieldError(field);
                }
            });
            
            // Validate email
            const emailFields = this.querySelectorAll('input[type="email"]');
            emailFields.forEach(field => {
                if (field.value && !isValidEmail(field.value)) {
                    isValid = false;
                    showFieldError(field, 'Email không đúng định dạng!');
                }
            });
            
            // Validate phone
            const phoneFields = this.querySelectorAll('input[name="phone"]');
            phoneFields.forEach(field => {
                if (field.value && !isValidPhone(field.value)) {
                    isValid = false;
                    showFieldError(field, 'Số điện thoại không hợp lệ!');
                }
            });
            
            if (!isValid) {
                e.preventDefault();
                showToast('Vui lòng điền đầy đủ thông tin!', 'error');
            } else {
                showLoading();
            }
        });
        
        // Real-time validation khi người dùng nhập
        form.querySelectorAll('input, select, textarea').forEach(field => {
            field.addEventListener('blur', function() {
                if (this.required && !this.value.trim()) {
                    showFieldError(this, 'Trường này không được để trống!');
                } else {
                    clearFieldError(this);
                }
            });
        });
    });
}

function showFieldError(field, message) {
    clearFieldError(field);
    field.style.borderColor = '#ef4444';
    const error = document.createElement('span');
    error.className = 'field-error';
    error.style.cssText = 'color:#ef4444;font-size:0.75rem;margin-top:4px;display:block;';
    error.textContent = message;
    field.parentNode.appendChild(error);
}

function clearFieldError(field) {
    field.style.borderColor = '';
    const error = field.parentNode.querySelector('.field-error');
    if (error) error.remove();
}

// ===================================
// 7. TOAST NOTIFICATIONS
// ===================================
function showToast(message, type = 'info') {
    // Xóa toast cũ nếu có
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();
    
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    const icons = { success: '✓', error: '✗', info: 'ℹ', warning: '⚠' };
    const colors = {
        success: '#10b981', 
        error: '#ef4444',
        info: '#38bdf8',
        warning: '#f59e0b'
    };
    
    toast.style.cssText = `
        position: fixed;
        top: 90px;
        right: 24px;
        padding: 14px 20px;
        background: #1a1a2e;
        border: 1px solid ${colors[type] || colors.info};
        border-radius: 10px;
        color: ${colors[type] || colors.info};
        font-size: 0.875rem;
        font-family: 'Poppins', sans-serif;
        display: flex;
        align-items: center;
        gap: 10px;
        z-index: 10000;
        box-shadow: 0 10px 40px rgba(0,0,0,0.5);
        animation: slideInDown 0.3s ease;
        max-width: 350px;
    `;
    
    toast.innerHTML = `<span style="font-weight:700">${icons[type] || icons.info}</span> ${message}`;
    document.body.appendChild(toast);
    
    // Tự động xóa sau 3 giây
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(10px)';
        toast.style.transition = 'all 0.3s';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ===================================
// 8. LOADING OVERLAY
// ===================================
function showLoading() {
    const overlay = document.querySelector('.loading-overlay');
    if (overlay) overlay.classList.add('active');
}

function hideLoading() {
    const overlay = document.querySelector('.loading-overlay');
    if (overlay) overlay.classList.remove('active');
}

// ===================================
// 9. PRINT INVOICE
// ===================================
function printInvoice() {
    window.print();
}

// ===================================
// 10. FILTER ROOMS
// ===================================
function initRoomFilter() {
    const filterBtns = document.querySelectorAll('.filter-btn');
    
    filterBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            filterBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            
            const type = this.dataset.type;
            const url = type ? `/rooms?type=${type}` : '/rooms';
            window.location.href = url;
        });
    });
}

// ===================================
// 11. COUNTER ANIMATION (Stats)
// ===================================
function animateCounters() {
    const counters = document.querySelectorAll('.stat-number[data-count]');
    
    counters.forEach(counter => {
        const target = parseInt(counter.dataset.count);
        const duration = 2000;
        const step = target / (duration / 16);
        let current = 0;
        
        const timer = setInterval(() => {
            current += step;
            if (current >= target) {
                current = target;
                clearInterval(timer);
            }
            counter.textContent = Math.floor(current) + (counter.dataset.suffix || '');
        }, 16);
    });
}

// ===================================
// 12. HELPER FUNCTIONS
// ===================================
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isValidPhone(phone) {
    return /^[0-9]{10,11}$/.test(phone.replace(/\s/g, ''));
}

// ===================================
// KHỞI TẠO KHI DOM READY
// ===================================
document.addEventListener('DOMContentLoaded', function() {
    initScrollReveal();
    initBookingCalculator();
    initPaymentMethods(); // Đã kích hoạt hàm xử lý thanh toán mới!
    initFormValidation();
    initRoomFilter();
    
    // Animate counters khi hiện ra màn hình
    const statsSection = document.querySelector('.hero-stats');
    if (statsSection) {
        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                animateCounters();
                observer.disconnect();
            }
        });
        observer.observe(statsSection);
    }
    
    // Auto-dismiss flash messages
    const alerts = document.querySelectorAll('.alert[data-auto-dismiss]');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            alert.style.transition = 'all 0.4s';
            setTimeout(() => alert.remove(), 400);
        }, 4000);
    });
    
    console.log('🏨 Hotel Management System initialized');
});