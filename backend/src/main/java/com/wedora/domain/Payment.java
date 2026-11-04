package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(columnNames = "stripe_payment_intent_id"))
@Getter
@Setter
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Booking booking;
    @Column(nullable = false)
    private Long amountCents;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "stripe_payment_intent_id", length = 100)
    private String stripePaymentIntentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
