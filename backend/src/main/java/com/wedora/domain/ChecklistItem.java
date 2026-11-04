package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "checklist_items", indexes = @Index(name = "idx_checklist_customer", columnList = "customer_id"))
@Getter
@Setter
@NoArgsConstructor
public class ChecklistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser customer;
    @Column(nullable = false, length = 180)
    private String title;
    @Column(length = 60)
    private String categorySlug;
    private LocalDate dueDate;
    @Column(nullable = false)
    private boolean completed = false;
    @Column(length = 2000)
    private String notes;
    @Column(length = 500)
    private String contractDetails;
    @Column(length = 40)
    private String paymentStatus;
    @Column(precision = 12, scale = 2)
    private BigDecimal budgetAmount;
    @ManyToOne(fetch = FetchType.EAGER)
    private Booking booking;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
