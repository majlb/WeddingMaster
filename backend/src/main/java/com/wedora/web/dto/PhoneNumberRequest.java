package com.wedora.web.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PhoneNumberRequest(
        @Size(max = 16) @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phoneNumber
) {}
