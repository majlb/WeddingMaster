package com.wedora.service;

import com.wedora.domain.AppUser;
import com.wedora.domain.CurrencyCode;
import com.wedora.domain.Role;
import com.wedora.domain.Supplier;
import com.wedora.repository.ChecklistRepository;
import com.wedora.repository.SupplierRepository;
import com.wedora.repository.UserRepository;
import com.wedora.security.JwtService;
import com.wedora.web.ApiException;
import com.wedora.web.dto.WeddingDateRequest;
import com.wedora.web.dto.CurrencyPreferenceRequest;
import com.wedora.web.dto.SupplierRegistrationRequest;
import com.wedora.web.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceWeddingDateTest {
    @Mock private UserRepository users;
    @Mock private SupplierRepository suppliers;
    @Mock private ChecklistRepository checklist;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthenticationService authenticationService;
    private AppUser couple;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(users, suppliers, checklist,
                passwordEncoder, jwtService);
        couple = new AppUser();
        couple.setId(5L);
        couple.setName("A Couple");
        couple.setEmail("couple@example.test");
        couple.setRole(Role.CUSTOMER);
        lenient().when(users.findByEmailIgnoreCase("couple@example.test")).thenReturn(Optional.of(couple));
    }

    @Test
    void coupleCanSaveAndClearWeddingDate() {
        LocalDate weddingDate = LocalDate.of(2027, 6, 12);
        when(jwtService.createToken(couple)).thenReturn("couple-token");

        var saved = authenticationService.updateWeddingDate("couple@example.test",
                new WeddingDateRequest(weddingDate));
        assertThat(couple.getWeddingDate()).isEqualTo(weddingDate);
        assertThat(saved.weddingDate()).isEqualTo(weddingDate);
        assertThat(saved.token()).isEqualTo("couple-token");

        var cleared = authenticationService.updateWeddingDate("couple@example.test",
                new WeddingDateRequest(null));
        assertThat(couple.getWeddingDate()).isNull();
        assertThat(cleared.weddingDate()).isNull();
    }

    @Test
    void supplierCannotSetACoupleWeddingDate() {
        couple.setRole(Role.SUPPLIER);

        assertThatThrownBy(() -> authenticationService.updateWeddingDate("couple@example.test",
                new WeddingDateRequest(LocalDate.of(2027, 6, 12))))
                .isInstanceOf(ApiException.Forbidden.class);

        verify(jwtService, never()).createToken(any());
    }

    @Test
    void supplierRegistrationDoesNotRequireOrAssignACategory() {
        when(passwordEncoder.encode("long-enough-password")).thenReturn("encoded");
        when(jwtService.createToken(any(AppUser.class))).thenReturn("supplier-token");
        when(suppliers.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new SupplierRegistrationRequest("Supplier", "supplier_name",
                "long-enough-password", "Studio", "Paris", null, java.util.List.of("FR", "BE"));

        var response = authenticationService.registerSupplier(request);

        var supplier = org.mockito.ArgumentCaptor.forClass(Supplier.class);
        verify(suppliers).save(supplier.capture());
        var savedUser = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(savedUser.capture());
        assertThat(response.role()).isEqualTo(Role.SUPPLIER);
        assertThat(supplier.getValue().getCategory()).isNull();
        assertThat(supplier.getValue().getCategories()).isEmpty();
        assertThat(savedUser.getValue().getUsername()).isEqualTo("supplier_name");
        assertThat(savedUser.getValue().getEmail()).isEqualTo("supplier_name@account.wedmaster.invalid");
        assertThat(response.email()).isNull();
    }

    @Test
    void usernameMustBeUniqueWhenRegistering() {
        when(users.existsByUsernameIgnoreCase("already-used")).thenReturn(true);
        var request = new SupplierRegistrationRequest("Supplier", "already-used",
                "long-enough-password", "Studio", "Paris", null, java.util.List.of("FR", "BE"));

        assertThatThrownBy(() -> authenticationService.registerSupplier(request))
                .isInstanceOf(ApiException.Conflict.class)
                .hasMessage("That username is already taken");
        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void coupleRegistrationUsesUsernameWithoutCollectingEmail() {
        when(users.existsByUsernameIgnoreCase("new_couple")).thenReturn(false);
        when(passwordEncoder.encode("long-enough-password")).thenReturn("encoded");
        when(jwtService.createToken(any(AppUser.class))).thenReturn("couple-token");

        var response = authenticationService.register(new com.wedora.web.dto.RegisterRequest(
                "New Couple", "new_couple", "long-enough-password", java.util.List.of("FR", "BE")));

        var savedUser = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(savedUser.capture());
        assertThat(savedUser.getValue().getEmail()).isEqualTo("new_couple@account.wedmaster.invalid");
        assertThat(response.email()).isNull();
        assertThat(response.username()).isEqualTo("new_couple");
    }

    @Test
    void userCanSignInWithUsernameInsteadOfEmail() {
        couple.setUsername("couple_name");
        couple.setPasswordHash("encoded");
        when(users.findByUsernameIgnoreCase("couple_name")).thenReturn(Optional.of(couple));
        when(passwordEncoder.matches("long-enough-password", "encoded")).thenReturn(true);
        when(jwtService.createToken(couple)).thenReturn("couple-token");

        var response = authenticationService.login(new LoginRequest("couple_name", "long-enough-password"));

        assertThat(response.token()).isEqualTo("couple-token");
        verify(users).findByUsernameIgnoreCase("couple_name");
    }

    @Test
    void userCanStillSignInWithEmail() {
        couple.setPasswordHash("encoded");
        when(passwordEncoder.matches("long-enough-password", "encoded")).thenReturn(true);
        when(jwtService.createToken(couple)).thenReturn("couple-token");

        var response = authenticationService.login(new LoginRequest("couple@example.test", "long-enough-password"));

        assertThat(response.token()).isEqualTo("couple-token");
        verify(users).findByEmailIgnoreCase("couple@example.test");
    }

    @Test
    void coupleCanSaveCurrencyPreference() {
        when(jwtService.createToken(couple)).thenReturn("couple-token");

        var saved = authenticationService.updateCurrency("couple@example.test",
                new CurrencyPreferenceRequest(CurrencyCode.EUR));

        assertThat(couple.getCurrencyCode()).isEqualTo(CurrencyCode.EUR);
        assertThat(saved.currencyCode()).isEqualTo("EUR");
        assertThat(saved.token()).isEqualTo("couple-token");
    }

    @Test
    void supplierCannotSetACoupleCurrencyPreference() {
        couple.setRole(Role.SUPPLIER);

        assertThatThrownBy(() -> authenticationService.updateCurrency("couple@example.test",
                new CurrencyPreferenceRequest(CurrencyCode.EUR)))
                .isInstanceOf(ApiException.Forbidden.class);

        verify(jwtService, never()).createToken(any());
    }
}
