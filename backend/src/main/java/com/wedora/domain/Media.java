package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "supplier_media")
@Getter
@Setter
@NoArgsConstructor
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Supplier supplier;
    @Column(nullable = false, length = 500)
    private String url;
    @Column(nullable = false, length = 20)
    private String mediaType;
    @Column(length = 180)
    private String caption;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
