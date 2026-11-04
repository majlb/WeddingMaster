package com.wedora.web.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record BookingRequest(
        @NotNull Long supplierId,
        Long serviceOfferingId,
        @NotNull @FutureOrPresent LocalDate eventDate,
        @Size(max = 3000) String notes
) {}
