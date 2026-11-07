package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "collaboration_members", uniqueConstraints =
        @UniqueConstraint(columnNames = {"collaboration_id", "supplier_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CollaborationMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "collaboration_id", nullable = false)
    private SupplierCollaboration collaboration;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CollaborationMemberStatus status = CollaborationMemberStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
