package com.wedora.repository;

import com.wedora.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import com.wedora.domain.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Booking> findBySupplierOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<Booking> findBySupplierId(Long supplierId);
    boolean existsByServiceOfferingId(Long serviceOfferingId);
    List<Booking> findByStatus(BookingStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
