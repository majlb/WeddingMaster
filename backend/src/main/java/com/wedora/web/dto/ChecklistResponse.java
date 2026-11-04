package com.wedora.web.dto;

import com.wedora.domain.ChecklistItem;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ChecklistResponse(
        Long id, String title, String categorySlug, LocalDate dueDate,
        boolean completed, String notes, String supplierName,
        String contractDetails, String paymentStatus, BigDecimal budgetAmount
) {
    public static ChecklistResponse from(ChecklistItem item) {
        return new ChecklistResponse(item.getId(), item.getTitle(), item.getCategorySlug(),
                item.getDueDate(), item.isCompleted(), item.getNotes(),
                item.getBooking() == null ? null : item.getBooking().getSupplier().getBusinessName(),
                item.getContractDetails(), item.getPaymentStatus(), item.getBudgetAmount());
    }
}
