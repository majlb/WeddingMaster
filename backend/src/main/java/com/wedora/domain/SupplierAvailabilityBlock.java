package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "supplier_availability_blocks",
        uniqueConstraints = @UniqueConstraint(name = "uq_offering_blocked_date",
                columnNames = {"service_offering_id", "blocked_date"}))
@Getter
@Setter
@NoArgsConstructor
public class SupplierAvailabilityBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_offering_id", nullable = false)
    private SupplierServiceOffering serviceOffering;

    @Column(name = "blocked_date", nullable = false)
    private LocalDate blockedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", unique = true)
    private Booking booking;
}
