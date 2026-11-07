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
        BigDecimal startingPrice, String currency, String contactEmail, String contactPhone, String whatsapp,
        String website, String facebook, String instagram, String tiktok,
        String mainImageUrl, List<String> countryCodes, Double rating, Integer reviewCount, boolean approved,
        boolean active,
        Map<String, Map<String, String>> localizedContent, List<CategoryResponse> categories
) {
    public static SupplierResponse from(Supplier supplier) {
        String language = LocaleContextHolder.getLocale().getLanguage();
        List<CategoryResponse> assignedCategories = supplier.getCategories().stream()
                .map(CategoryResponse::from)
                .toList();
        var primaryCategory = supplier.getCategory() != null ? supplier.getCategory()
                : supplier.getCategories().stream().findFirst().orElse(null);
        return new SupplierResponse(
                supplier.getId(),
                TranslationSupport.value(supplier.getLocalizedContent(), language, "businessName", supplier.getBusinessName()),
                supplier.getSlug(),
                primaryCategory == null ? null : TranslationSupport.value(primaryCategory.getTranslations(), language,
                        "name", primaryCategory.getName()),
                primaryCategory == null ? null : primaryCategory.getSlug(),
                TranslationSupport.value(supplier.getLocalizedContent(), language, "location", supplier.getLocation()),
                supplier.getLatitude(), supplier.getLongitude(),
                TranslationSupport.value(supplier.getLocalizedContent(), language, "description", supplier.getDescription()),
                TranslationSupport.value(supplier.getLocalizedContent(), language, "services", supplier.getServices()),
                TranslationSupport.value(supplier.getLocalizedContent(), language, "availability", supplier.getAvailability()),
                supplier.getStartingPrice(), supplier.getCurrency(),
                supplier.getContactEmail(), supplier.getContactPhone(), supplier.getWhatsapp(),
                supplier.getWebsite(), supplier.getFacebook(), supplier.getInstagram(), supplier.getTiktok(),
                supplier.getMainImageUrl(),
                supplier.getCountryCodes(), supplier.getRating(),
                supplier.getReviewCount(), supplier.isApproved(), supplier.isActive(),
                supplier.getLocalizedContent(), assignedCategories);
    }
}
