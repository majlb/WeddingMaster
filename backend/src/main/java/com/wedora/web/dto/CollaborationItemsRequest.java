package com.wedora.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CollaborationItemsRequest(@NotEmpty List<@NotNull Long> serviceIds) {}
