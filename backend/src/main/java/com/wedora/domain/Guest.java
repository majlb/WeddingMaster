package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "guests")
@Getter
@Setter
@NoArgsConstructor
public class Guest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser weddingCouple;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer personCount;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false)
    private boolean approved = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
