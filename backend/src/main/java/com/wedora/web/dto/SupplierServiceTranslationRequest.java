package com.wedora.web.dto;

import jakarta.validation.constraints.Size;

public record SupplierServiceTranslationRequest(
        @Size(max = 150) String name,
        @Size(max = 2000) String description
) {}
