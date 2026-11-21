package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "booking_history")
@Getter
@NoArgsConstructor
public class BookingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Booking booking;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;
    @Column(nullable = false, length = 20)
    private String actor;
    @Column(length = 3000)
    private String comment;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BookingHistory(Booking booking, BookingStatus status, String actor, String comment) {
        this.booking = booking;
        this.status = status;
        this.actor = actor;
        this.comment = comment;
    }
}
