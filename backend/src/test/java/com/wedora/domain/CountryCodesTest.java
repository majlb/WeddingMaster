package com.wedora.domain;

import com.wedora.web.ApiException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountryCodesTest {
    @Test
    void normalizesAndRemovesDuplicateCountryCodes() {
        assertThat(CountryCodes.normalize(List.of("us", "FR", "us"))).containsExactly("US", "FR");
    }

    @Test
    void rejectsUnknownCountryCodes() {
        assertThatThrownBy(() -> CountryCodes.normalize(List.of("ZZ")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void exposesCountryOptionsForTheIsoCountryCatalog() {
        assertThat(CountryCodes.all()).extracting(CountryCodes.CountryOption::code).contains("US", "FR", "GR", "AR");
    }
}
