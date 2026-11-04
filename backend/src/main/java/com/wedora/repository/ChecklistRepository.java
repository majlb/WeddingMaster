package com.wedora.repository;

import com.wedora.domain.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChecklistRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByCustomerIdOrderByCreatedAtAsc(Long customerId);
    List<ChecklistItem> findByBookingId(Long bookingId);
    long deleteByIdAndCustomerId(Long id, Long customerId);
}
