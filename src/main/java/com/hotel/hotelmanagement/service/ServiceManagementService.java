package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.Service;
import com.hotel.hotelmanagement.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ServiceManagementService {

    @Autowired
    private ServiceRepository serviceRepository;

    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    // Danh sách dịch vụ đang hoạt động, hiển thị cho user chọn khi đặt phòng
    public List<Service> getActiveServices() {
        return serviceRepository.findByActiveTrue();
    }

    public Optional<Service> getServiceById(Long id) {
        return serviceRepository.findById(id);
    }

    public Service saveService(Service service) {
        return serviceRepository.save(service);
    }

    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }

    // Lấy danh sách dịch vụ theo danh sách ID (dùng khi xử lý booking)
    public List<Service> getServicesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return serviceRepository.findAllById(ids);
    }
}
