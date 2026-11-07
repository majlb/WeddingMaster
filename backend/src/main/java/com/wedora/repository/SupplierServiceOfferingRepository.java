package com.wedora.repository;

import com.wedora.domain.SupplierServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface SupplierServiceOfferingRepository extends JpaRepository<SupplierServiceOffering, Long> {
    List<SupplierServiceOffering> findBySupplierIdOrderByIdAsc(Long supplierId);
    Optional<SupplierServiceOffering> findByIdAndSupplierId(Long id, Long supplierId);
    boolean existsBySupplierIdAndCategoryId(Long supplierId, Long categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select offering from SupplierServiceOffering offering where offering.id = :id")
    Optional<SupplierServiceOffering> findByIdForUpdate(@Param("id") Long id);
}
