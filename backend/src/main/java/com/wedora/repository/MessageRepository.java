package com.wedora.repository;

import com.wedora.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
    long countByBookingSupplierId(Long supplierId);
}
