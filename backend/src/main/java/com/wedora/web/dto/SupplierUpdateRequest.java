package com.wedora.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SupplierUpdateRequest(
        @Size(max = 150) String businessName,
        @Size(max = 180) String location,
        @Size(max = 4000) String description,
        @Size(max = 500) String services,
        @Size(max = 8000) String availability,
        BigDecimal startingPrice,
        @Size(min = 3, max = 3) String currency,
        @Email @Size(max = 180) String contactEmail,
        @Size(max = 30) String contactPhone,
        @Size(max = 30) String whatsapp,
        @Size(max = 180) String website,
        @Size(max = 500) String facebook,
        @Size(max = 500) String instagram,
        @Size(max = 500) String tiktok,
        @Valid Map<String, SupplierLocalizedContent> localizedContent,
        List<@Size(min = 2, max = 2) String> countryCodes,
        Double latitude,
        Double longitude
) {}
