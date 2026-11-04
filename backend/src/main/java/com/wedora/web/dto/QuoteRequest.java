package com.wedora.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuoteRequest(@NotNull @Positive Long amountCents, String currency) {}
