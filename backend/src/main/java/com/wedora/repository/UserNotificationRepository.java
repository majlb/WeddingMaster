package com.wedora.repository;

import com.wedora.domain.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findTop50ByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    long countByRecipientIdAndReadAtIsNull(Long recipientId);
    Optional<UserNotification> findByIdAndRecipientId(Long id, Long recipientId);
    List<UserNotification> findByRecipientIdAndReadAtIsNull(Long recipientId);
}
