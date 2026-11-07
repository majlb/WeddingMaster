package com.wedora.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SupplierRegistrationRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String username,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotBlank @Size(max = 150) String businessName,
        @NotBlank @Size(max = 180) String location,
        @Size(max = 4000) String description,
        @NotEmpty List<@Size(min = 2, max = 2) String> countryCodes
) {}
