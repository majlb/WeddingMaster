package com.wedora.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record GuestRequest(
        @NotBlank(message = "Guest name is required")
        String name,
        @Positive(message = "Person count must be greater than 0")
        Integer personCount,
        String phone
) {}
