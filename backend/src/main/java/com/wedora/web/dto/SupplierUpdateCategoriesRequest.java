package com.wedora.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SupplierUpdateCategoriesRequest(@NotNull List<@NotNull Long> categoryIds) {}
