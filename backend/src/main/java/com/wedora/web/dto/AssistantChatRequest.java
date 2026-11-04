package com.wedora.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AssistantChatRequest(
        @NotEmpty @Size(max = 12) List<@Valid Message> messages
) {
    public record Message(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 1200) String content
    ) {}
}
