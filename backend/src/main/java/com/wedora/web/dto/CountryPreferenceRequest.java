package com.wedora.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CountryPreferenceRequest(
        @NotEmpty List<@Size(min = 2, max = 2) String> countryCodes
) {}
