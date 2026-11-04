package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings", indexes = @Index(name = "idx_booking_customer_status", columnList = "customer_id,status"))
@Getter
@Setter
@NoArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser customer;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Supplier supplier;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_offering_id")
    private SupplierServiceOffering serviceOffering;
    @Column(nullable = false)
    private LocalDate eventDate;
    @Column(length = 3000)
    private String notes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.REQUESTED;
    private Long quotedAmountCents;
    @Column(length = 3)
    private String currency = "EUR";
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
