package com.wedora.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ChecklistRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 60) String categorySlug,
        LocalDate dueDate,
        @Size(max = 2000) String notes,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal budgetAmount
) {}
