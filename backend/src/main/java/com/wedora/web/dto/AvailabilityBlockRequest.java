package com.wedora.web.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AvailabilityBlockRequest(@NotNull @FutureOrPresent LocalDate date) {}
