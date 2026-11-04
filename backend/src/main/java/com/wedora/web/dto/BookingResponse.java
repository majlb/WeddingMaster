package com.wedora.web.dto;

import com.wedora.domain.Booking;
import com.wedora.domain.BookingStatus;
import com.wedora.domain.TranslationSupport;
import org.springframework.context.i18n.LocaleContextHolder;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        Long id, Long supplierId, String supplierName, String serviceOfferingName, String customerName, String categorySlug,
        LocalDate eventDate, String notes, BookingStatus status,
        Long quotedAmountCents, String currency, Instant createdAt
) {
    public static BookingResponse from(Booking booking) {
        String language = LocaleContextHolder.getLocale().getLanguage();
        return new BookingResponse(booking.getId(), booking.getSupplier().getId(),
                booking.getSupplier().getBusinessName(),
                booking.getServiceOffering() == null ? null
                        : TranslationSupport.value(booking.getServiceOffering().getLocalizedContent(), language,
                        "name", booking.getServiceOffering().getName()),
                booking.getCustomer().getName(),
                booking.getSupplier().getCategory().getSlug(),
                booking.getEventDate(), booking.getNotes(), booking.getStatus(),
                booking.getQuotedAmountCents(), booking.getCurrency(), booking.getCreatedAt());
    }
}
