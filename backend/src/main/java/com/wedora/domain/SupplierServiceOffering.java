package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "supplier_services")
@Getter
@Setter
@NoArgsConstructor
public class SupplierServiceOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Supplier supplier;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 2000)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "localized_content", columnDefinition = "jsonb", nullable = false)
    private Map<String, Map<String, String>> localizedContent = new HashMap<>();

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String imageUrl;
}
