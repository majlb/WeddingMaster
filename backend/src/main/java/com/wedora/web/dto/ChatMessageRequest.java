package com.wedora.web.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record ChatMessageRequest(@Size(max = 4000) String body, @Positive Long attachmentId) {}
