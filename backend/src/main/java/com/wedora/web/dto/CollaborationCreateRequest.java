package com.wedora.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record CollaborationCreateRequest(
        @NotNull Long supplierId,
        @NotEmpty Map<String, String> names,
        @NotEmpty List<@NotNull Long> invitedSupplierIds,
        @NotEmpty List<@NotNull Long> serviceIds
) {}
