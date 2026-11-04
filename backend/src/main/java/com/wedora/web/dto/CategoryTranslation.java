package com.wedora.web.dto;

import jakarta.validation.constraints.Size;

public record CategoryTranslation(
        @Size(max = 90) String name,
        @Size(max = 300) String description
) {}
