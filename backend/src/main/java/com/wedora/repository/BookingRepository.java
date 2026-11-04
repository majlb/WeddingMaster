package com.wedora.repository;

import com.wedora.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Booking> findBySupplierOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<Booking> findBySupplierId(Long supplierId);
    boolean existsByServiceOfferingId(Long serviceOfferingId);
}
