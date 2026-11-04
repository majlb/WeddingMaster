package com.wedora.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.Map;

public record CategoryRequest(
        @NotBlank @Size(max = 90) String name,
        @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 100) String slug,
        @Size(max = 300) String description,
        @Valid Map<String, CategoryTranslation> translations
) {}
