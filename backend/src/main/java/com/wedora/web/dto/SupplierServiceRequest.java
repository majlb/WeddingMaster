package com.wedora.web.dto;

import com.wedora.domain.PricingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

public record SupplierServiceRequest(
        Map<String, SupplierServiceTranslationRequest> translations,
        @DecimalMin("0.00") BigDecimal price,
        @NotNull Long categoryId,
        PricingType pricingType,
        @Min(1) Integer minimumAmount,
        @Min(1) Integer maximumAmount
) {}
