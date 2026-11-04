package com.wedora.web;

import com.wedora.service.AuthenticationService;
import com.wedora.web.dto.AuthResponse;
import com.wedora.web.dto.CountryPreferenceRequest;
import com.wedora.web.dto.CurrencyPreferenceRequest;
import com.wedora.web.dto.WeddingDateRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {
    private final AuthenticationService authenticationService;

    public UserProfileController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PutMapping("/countries")
    public AuthResponse updateCountries(@AuthenticationPrincipal String email,
                                        @Valid @RequestBody CountryPreferenceRequest request) {
        return authenticationService.updateCountries(email, request);
    }

    @PutMapping("/wedding-date")
    public AuthResponse updateWeddingDate(@AuthenticationPrincipal String email,
                                          @RequestBody WeddingDateRequest request) {
        return authenticationService.updateWeddingDate(email, request);
    }

    @PutMapping("/currency")
    public AuthResponse updateCurrency(@AuthenticationPrincipal String email,
                                       @Valid @RequestBody CurrencyPreferenceRequest request) {
        return authenticationService.updateCurrency(email, request);
    }
}
