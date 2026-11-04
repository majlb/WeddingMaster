package com.wedora.service;

import com.wedora.domain.AppUser;
import com.wedora.domain.CurrencyCode;
import com.wedora.domain.Role;
import com.wedora.repository.CategoryRepository;
import com.wedora.repository.ChecklistRepository;
import com.wedora.repository.SupplierRepository;
import com.wedora.repository.UserRepository;
import com.wedora.security.JwtService;
import com.wedora.web.ApiException;
import com.wedora.web.dto.WeddingDateRequest;
import com.wedora.web.dto.CurrencyPreferenceRequest;
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
    @Mock private CategoryRepository categories;
    @Mock private ChecklistRepository checklist;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthenticationService authenticationService;
    private AppUser couple;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(users, suppliers, categories, checklist,
                passwordEncoder, jwtService);
        couple = new AppUser();
        couple.setId(5L);
        couple.setName("A Couple");
        couple.setEmail("couple@example.test");
        couple.setRole(Role.CUSTOMER);
        when(users.findByEmailIgnoreCase("couple@example.test")).thenReturn(Optional.of(couple));
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
