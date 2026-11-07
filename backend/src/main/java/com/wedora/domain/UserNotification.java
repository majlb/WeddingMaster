package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_notifications", indexes = {
        @Index(name = "idx_notification_recipient_created", columnList = "recipient_id,created_at"),
        @Index(name = "idx_notification_recipient_read", columnList = "recipient_id,read_at")
})
@Getter
@Setter
@NoArgsConstructor
public class UserNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser recipient;
    @ManyToOne(fetch = FetchType.LAZY)
    private Booking booking;
    @Column(name = "collaboration_id")
    private Long collaborationId;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(nullable = false, length = 120)
    private String actorName;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    private Instant readAt;
}
