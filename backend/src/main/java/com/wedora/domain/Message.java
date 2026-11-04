package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "messages", indexes = @Index(name = "idx_message_booking_created", columnList = "booking_id,created_at"))
@Getter
@Setter
@NoArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Booking booking;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private AppUser sender;
    @Column(length = 4000)
    private String body;
    @Column(length = 500)
    private String attachmentUrl;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
