package com.wedora.web.dto;

import com.wedora.domain.SupplierServiceOffering;
import com.wedora.domain.TranslationSupport;
import org.springframework.context.i18n.LocaleContextHolder;

import java.math.BigDecimal;
import java.util.Map;

public record SupplierServiceResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        String imageUrl,
        Map<String, Map<String, String>> localizedContent,
        Long categoryId,
        String categoryName,
        String categorySlug
) {
    public static SupplierServiceResponse from(SupplierServiceOffering service) {
        String language = LocaleContextHolder.getLocale().getLanguage();
        return new SupplierServiceResponse(
                service.getId(),
                TranslationSupport.value(service.getLocalizedContent(), language, "name", service.getName()),
                TranslationSupport.value(service.getLocalizedContent(), language, "description", service.getDescription()),
                service.getPrice(),
                service.getSupplier().getCurrency(),
                service.getImageUrl(),
                service.getLocalizedContent(),
                service.getCategory().getId(),
                TranslationSupport.value(service.getCategory().getTranslations(), language,
                        "name", service.getCategory().getName()),
                service.getCategory().getSlug());
    }
}
