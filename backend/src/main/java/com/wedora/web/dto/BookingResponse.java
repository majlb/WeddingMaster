package com.wedora.web.dto;

import com.wedora.domain.Booking;
import com.wedora.domain.BookingStatus;
import com.wedora.domain.TranslationSupport;
import org.springframework.context.i18n.LocaleContextHolder;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import com.wedora.domain.PricingType;

public record BookingResponse(
        Long id, Long supplierId, String supplierName, String serviceOfferingName, String customerName, String categorySlug,
        LocalDate eventDate, String notes, BookingStatus status,
        Long quotedAmountCents, String currency, Instant createdAt,
        Integer quantity, BigDecimal servicePrice, String servicePriceCurrency, PricingType servicePricingType,
        List<HistoryEntry> history
) {
    public record HistoryEntry(BookingStatus status, String actor, String comment, Instant createdAt) {}
    public static BookingResponse from(Booking booking) {
        String language = LocaleContextHolder.getLocale().getLanguage();
        var category = booking.getServiceOffering() == null
                ? (booking.getSupplier().getCategory() != null ? booking.getSupplier().getCategory()
                : booking.getSupplier().getCategories().stream().findFirst().orElse(null))
                : booking.getServiceOffering().getCategory();
        return new BookingResponse(booking.getId(), booking.getSupplier().getId(),
                booking.getSupplier().getBusinessName(),
                booking.getServiceOffering() == null ? null
                        : TranslationSupport.value(booking.getServiceOffering().getLocalizedContent(), language,
                        "name", booking.getServiceOffering().getName()),
                booking.getCustomer().getName(),
                category == null ? null : category.getSlug(),
                booking.getEventDate(), booking.getNotes(), booking.getStatus(),
                booking.getQuotedAmountCents(), booking.getCurrency(), booking.getCreatedAt(),
                booking.getQuantity(), booking.getServicePrice(), booking.getServicePriceCurrency(), booking.getServicePricingType(),
                booking.getHistory().stream().map(entry -> new HistoryEntry(entry.getStatus(),
                        entry.getActor(), entry.getComment(), entry.getCreatedAt())).toList());
    }
}
