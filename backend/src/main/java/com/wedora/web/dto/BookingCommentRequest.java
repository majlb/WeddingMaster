package com.wedora.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookingCommentRequest(@NotBlank @Size(max = 3000) String comment) {}
