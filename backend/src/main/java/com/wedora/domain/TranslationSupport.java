package com.wedora.domain;

import java.util.Map;

public final class TranslationSupport {
    private TranslationSupport() {}

    public static String value(Map<String, Map<String, String>> translations,
                               String language, String field, String fallback) {
        if (translations == null) return fallback;
        Map<String, String> localized = translations.get(language);
        if (localized == null || localized.get(field) == null || localized.get(field).isBlank()) {
            return fallback;
        }
        return localized.get(field);
    }
}
