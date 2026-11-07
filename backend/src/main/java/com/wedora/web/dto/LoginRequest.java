package com.wedora.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@JsonAlias("email") @NotBlank @Size(max = 180) String username,
                           @NotBlank String password) {}
