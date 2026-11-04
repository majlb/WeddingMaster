package com.wedora.web.dto;

import java.time.Instant;

public record InvoiceResponse(
        String invoiceNumber, Instant issuedAt, Long bookingId,
        String customerName, String supplierName, String eventDate,
        Long amountCents, String currency, String paymentReference
) {}
