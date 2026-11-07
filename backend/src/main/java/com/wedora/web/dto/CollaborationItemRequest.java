package com.wedora.web.dto;

import jakarta.validation.constraints.NotNull;

public record CollaborationItemRequest(@NotNull Long serviceId) {}
