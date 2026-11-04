package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "chat_attachments")
@Getter
@Setter
@NoArgsConstructor
public class ChatAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Booking booking;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser uploader;
    @Column(nullable = false, length = 200)
    private String storageKey;
    @Column(nullable = false, length = 180)
    private String fileName;
    @Column(nullable = false, length = 100)
    private String contentType;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
