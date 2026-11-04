package com.wedora.web.dto;

import com.wedora.domain.Supplier;
import com.wedora.domain.TranslationSupport;
import org.springframework.context.i18n.LocaleContextHolder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SupplierResponse(
        Long id, String businessName, String slug, String categoryName, String categorySlug,
        String location, Double latitude, Double longitude, String description, String services, String availability,
        BigDecimal startingPrice, String currency, String contactPhone, String website,
        String mainImageUrl, List<String> countryCodes, Double rating, Integer reviewCount, boolean approved,
        Map<String, Map<String, String>> localizedContent
) {
    public static SupplierResponse from(Supplier supplier) {
        String language = LocaleContextHolder.getLocale().getLanguage();
        return new SupplierResponse(
                supplier.getId(),
                TranslationSupport.value(supplier.getLocalizedContent(), language, "businessName", supplier.getBusinessName()),
                supplier.getSlug(),
                TranslationSupport.value(supplier.getCategory().getTranslations(), language, "name", supplier.getCategory().getName()),
                supplier.getCategory().getSlug(),
                TranslationSupport.value(supplier.getLocalizedContent(), language, "location", supplier.getLocation()),
                supplier.getLatitude(), supplier.getLongitude(),
                TranslationSupport.value(supplier.getLocalizedContent(), language, "description", supplier.getDescription()),
                TranslationSupport.value(supplier.getLocalizedContent(), language, "services", supplier.getServices()),
                TranslationSupport.value(supplier.getLocalizedContent(), language, "availability", supplier.getAvailability()),
                supplier.getStartingPrice(), supplier.getCurrency(),
                supplier.getContactPhone(), supplier.getWebsite(), supplier.getMainImageUrl(),
                supplier.getCountryCodes(), supplier.getRating(),
                supplier.getReviewCount(), supplier.isApproved(), supplier.getLocalizedContent());
    }
}
