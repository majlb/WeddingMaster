package com.wedora.web.dto;

import jakarta.validation.constraints.NotNull;

public record CollaborationInviteRequest(@NotNull Long supplierId) {}
