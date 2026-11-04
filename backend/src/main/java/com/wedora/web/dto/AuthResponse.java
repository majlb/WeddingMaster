package com.wedora.web.dto;

import com.wedora.domain.Role;
import java.time.LocalDate;
import java.util.List;

public record AuthResponse(String token, Long id, String name, String email, Role role,
                           List<String> countryCodes, LocalDate weddingDate, String currencyCode) {}
