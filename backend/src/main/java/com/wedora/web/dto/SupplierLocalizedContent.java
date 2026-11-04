package com.wedora.web.dto;

import jakarta.validation.constraints.Size;

public record SupplierLocalizedContent(
        @Size(max = 150) String businessName,
        @Size(max = 180) String location,
        @Size(max = 4000) String description,
        @Size(max = 500) String services,
        @Size(max = 8000) String availability
) {}
