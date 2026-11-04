package com.wedora.service;

import com.wedora.domain.AppUser;
import com.wedora.domain.Category;
import com.wedora.domain.CountryCodes;
import com.wedora.domain.CurrencyCode;
import com.wedora.domain.Role;
import com.wedora.domain.Supplier;
import com.wedora.repository.CategoryRepository;
import com.wedora.repository.ChecklistRepository;
import com.wedora.repository.SupplierRepository;
import com.wedora.repository.UserRepository;
import com.wedora.security.JwtService;
import com.wedora.web.ApiException;
import com.wedora.web.dto.AuthResponse;
import com.wedora.web.dto.LoginRequest;
import com.wedora.web.dto.RegisterRequest;
import com.wedora.web.dto.SupplierRegistrationRequest;
import com.wedora.web.dto.CountryPreferenceRequest;
import com.wedora.web.dto.CurrencyPreferenceRequest;
import com.wedora.web.dto.WeddingDateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.List;

@Service
public class AuthenticationService {
    private final UserRepository users;
    private final SupplierRepository suppliers;
    private final CategoryRepository categories;
    private final ChecklistRepository checklist;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(UserRepository users, SupplierRepository suppliers,
                                 CategoryRepository categories, ChecklistRepository checklist, PasswordEncoder passwordEncoder,
                                 JwtService jwtService) {
        this.users = users;
        this.suppliers = suppliers;
        this.categories = categories;
        this.checklist = checklist;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        ensureEmailAvailable(request.email());
        AppUser user = new AppUser();
        user.setName(request.name().trim());
        user.setEmail(normalizeEmail(request.email()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        user.setCountryCodes(CountryCodes.normalize(request.countryCodes()));
        users.save(user);
        seedChecklist(user);
        return response(user);
    }

    @Transactional
    public AuthResponse registerSupplier(SupplierRegistrationRequest request) {
        ensureEmailAvailable(request.email());
        Category category = categories.findBySlugAndActiveTrue(request.categorySlug())
                .orElseThrow(() -> new ApiException.NotFound("Supplier category not found"));
        AppUser user = new AppUser();
        user.setName(request.name().trim());
        user.setEmail(normalizeEmail(request.email()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.SUPPLIER);
        users.save(user);

        Supplier supplier = new Supplier();
        supplier.setOwner(user);
        supplier.setCategory(category);
        supplier.setBusinessName(request.businessName().trim());
        supplier.setSlug(createSlug(request.businessName()));
        supplier.setLocation(request.location().trim());
        supplier.setCountryCodes(CountryCodes.normalize(request.countryCodes()));
        supplier.setDescription(request.description());
        supplier.setApproved(false);
        suppliers.save(supplier);
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ApiException.Unauthorized("Email or password is incorrect"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException.Unauthorized("Email or password is incorrect");
        }
        return response(user);
    }

    @Transactional
    public AuthResponse updateCountries(String email, CountryPreferenceRequest request) {
        AppUser user = users.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
        if (user.getRole() != Role.CUSTOMER) {
            throw new ApiException.Forbidden("Only couple accounts can update couple country preferences");
        }
        user.setCountryCodes(CountryCodes.normalize(request.countryCodes()));
        return response(user);
    }

    @Transactional
    public AuthResponse updateWeddingDate(String email, WeddingDateRequest request) {
        AppUser user = users.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
        if (user.getRole() != Role.CUSTOMER) {
            throw new ApiException.Forbidden("Only couple accounts can update the wedding date");
        }
        user.setWeddingDate(request.weddingDate());
        return response(user);
    }

    @Transactional
    public AuthResponse updateCurrency(String email, CurrencyPreferenceRequest request) {
        AppUser user = users.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
        if (user.getRole() != Role.CUSTOMER) {
            throw new ApiException.Forbidden("Only couple accounts can update their currency preference");
        }
        user.setCurrencyCode(request.currencyCode());
        return response(user);
    }

    private void ensureEmailAvailable(String email) {
        if (users.existsByEmailIgnoreCase(normalizeEmail(email))) {
            throw new ApiException.Conflict("An account with this email already exists");
        }
    }

    private AuthResponse response(AppUser user) {
        return new AuthResponse(jwtService.createToken(user), user.getId(), user.getName(), user.getEmail(),
                user.getRole(), user.getCountryCodes(), user.getWeddingDate(),
                user.getCurrencyCode() == null ? CurrencyCode.USD.name() : user.getCurrencyCode().name());
    }

    private void seedChecklist(AppUser customer) {
        List<String[]> tasks = List.of(
                new String[]{"Find your wedding venue", "venues"},
                new String[]{"Book your caterer", "catering"},
                new String[]{"Choose your florist", "flowers"},
                new String[]{"Find your wedding dress", "dresses"},
                new String[]{"Book your hairstylist", "hair"},
                new String[]{"Book your makeup artist", "makeup"},
                new String[]{"Book your photographer", "photography"}
        );
        for (String[] task : tasks) {
            var item = new com.wedora.domain.ChecklistItem();
            item.setCustomer(customer);
            item.setTitle(task[0]);
            item.setCategorySlug(task[1]);
            checklist.save(item);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String createSlug(String name) {
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
