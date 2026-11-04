package com.wedora.web.dto;

import com.wedora.domain.CurrencyCode;
import jakarta.validation.constraints.NotNull;

public record CurrencyPreferenceRequest(@NotNull CurrencyCode currencyCode) {
}
