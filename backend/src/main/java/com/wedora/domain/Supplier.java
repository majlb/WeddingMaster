package com.wedora.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "suppliers", indexes = {
        @Index(name = "idx_supplier_category_approved", columnList = "category_id,approved"),
        @Index(name = "idx_supplier_location", columnList = "location")
})
@Getter
@Setter
@NoArgsConstructor
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser owner;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Category category;
    @Column(nullable = false, length = 150)
    private String businessName;
    @Column(nullable = false, length = 140)
    private String slug;
    @Column(nullable = false, length = 180)
    private String location;
    @Column(name = "latitude")
    private Double latitude;
    @Column(name = "longitude")
    private Double longitude;
    @Column(length = 4000)
    private String description;
    @Column(length = 500)
    private String services;
    @Column(length = 8000)
    private String availability;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "localized_content", columnDefinition = "jsonb", nullable = false)
    private Map<String, Map<String, String>> localizedContent = new HashMap<>();
    @Column(precision = 12, scale = 2)
    private BigDecimal startingPrice;
    @Column(length = 3)
    private String currency = "EUR";
    @Column(length = 30)
    private String contactPhone;
    @Column(length = 180)
    private String website;
    @Column(name = "main_image_url", length = 500)
    private String mainImageUrl;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "country_codes", columnDefinition = "jsonb", nullable = false)
    private List<String> countryCodes = new ArrayList<>();
    private Double rating = 0.0;
    private Integer reviewCount = 0;
    @Column(nullable = false)
    private Long profileVisits = 0L;
    @Column(nullable = false)
    private boolean approved = false;
    @Column(nullable = false)
    private boolean active = true;
}
