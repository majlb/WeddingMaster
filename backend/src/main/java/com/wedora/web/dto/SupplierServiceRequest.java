package com.wedora.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

public record SupplierServiceRequest(
        Map<String, SupplierServiceTranslationRequest> translations,
        @DecimalMin("0.00") BigDecimal price,
        @NotNull Long categoryId
) {}
