package com.wedora.domain;

import com.wedora.web.ApiException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class CountryCodes {
    private static final List<String> ISO_CODES = Arrays.asList(Locale.getISOCountries());

    private CountryCodes() {}

    public static List<String> normalize(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Select at least one country");
        }
        List<String> normalized = codes.stream()
                .map(code -> code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (normalized.size() > ISO_CODES.size() || normalized.stream().anyMatch(code -> !ISO_CODES.contains(code))) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Select valid ISO country codes");
        }
        return normalized;
    }

    public static List<CountryOption> all() {
        return ISO_CODES.stream()
                .map(code -> new CountryOption(code, new Locale("", code).getDisplayCountry(Locale.ENGLISH)))
                .filter(country -> !country.name().isBlank())
                .sorted((left, right) -> left.name().compareToIgnoreCase(right.name()))
                .toList();
    }

    public record CountryOption(String code, String name) {}
}
