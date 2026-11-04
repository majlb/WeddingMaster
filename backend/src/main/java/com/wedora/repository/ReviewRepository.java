package com.wedora.repository;

import com.wedora.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookingSupplierIdOrderByCreatedAtDesc(Long supplierId);
    boolean existsByBookingIdAndAuthorId(Long bookingId, Long authorId);
}
