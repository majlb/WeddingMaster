package com.wedora.repository;

import com.wedora.domain.SupplierAvailabilityBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SupplierAvailabilityBlockRepository extends JpaRepository<SupplierAvailabilityBlock, Long> {
    List<SupplierAvailabilityBlock> findByServiceOfferingIdAndBlockedDateBetweenOrderByBlockedDateAsc(
            Long serviceOfferingId, LocalDate from, LocalDate to);

    Optional<SupplierAvailabilityBlock> findByServiceOfferingIdAndBlockedDate(
            Long serviceOfferingId, LocalDate blockedDate);

    boolean existsByServiceOfferingIdAndBlockedDate(Long serviceOfferingId, LocalDate blockedDate);

    void deleteByServiceOfferingId(Long serviceOfferingId);
}
