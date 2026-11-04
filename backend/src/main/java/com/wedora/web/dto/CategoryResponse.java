package com.wedora.web.dto;

import com.wedora.domain.Category;
import com.wedora.domain.TranslationSupport;
import org.springframework.context.i18n.LocaleContextHolder;
import java.util.Map;

public record CategoryResponse(Long id, String name, String slug, String description,
                               Map<String, Map<String, String>> translations) {
    public static CategoryResponse from(Category category) {
        String language = LocaleContextHolder.getLocale().getLanguage();
        return new CategoryResponse(category.getId(),
                TranslationSupport.value(category.getTranslations(), language, "name", category.getName()),
                category.getSlug(),
                TranslationSupport.value(category.getTranslations(), language, "description", category.getDescription()),
                category.getTranslations());
    }
}
