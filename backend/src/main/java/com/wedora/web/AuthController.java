package com.wedora.web;

import com.wedora.service.AuthenticationService;
import com.wedora.web.dto.AuthResponse;
import com.wedora.web.dto.LoginRequest;
import com.wedora.web.dto.RegisterRequest;
import com.wedora.web.dto.SupplierRegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.register(request);
    }

    @PostMapping("/register-supplier")
    public AuthResponse registerSupplier(@Valid @RequestBody SupplierRegistrationRequest request) {
        return authenticationService.registerSupplier(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }
}
