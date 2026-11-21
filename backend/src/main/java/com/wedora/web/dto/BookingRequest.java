package com.wedora.web.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record BookingRequest(
        @NotNull Long supplierId,
        Long serviceOfferingId,
        @NotNull @FutureOrPresent LocalDate eventDate,
        @Size(max = 2000) String notes,
        @NotNull @Positive Integer quantity
) {}
