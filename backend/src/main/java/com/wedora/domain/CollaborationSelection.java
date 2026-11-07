package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "collaboration_selections", uniqueConstraints =
        @UniqueConstraint(columnNames = {"collaboration_id", "customer_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CollaborationSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "collaboration_id", nullable = false)
    private SupplierCollaboration collaboration;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private AppUser customer;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
