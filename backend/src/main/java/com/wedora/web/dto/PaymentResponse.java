package com.wedora.web.dto;

import com.wedora.domain.Payment;
import com.wedora.domain.PaymentStatus;
import java.time.Instant;

public record PaymentResponse(Long id, Long bookingId, Long amountCents, String currency,
                              PaymentStatus status, String clientSecret, Instant createdAt) {
    public static PaymentResponse from(Payment payment, String clientSecret) {
        return new PaymentResponse(payment.getId(), payment.getBooking().getId(),
                payment.getAmountCents(), payment.getCurrency(), payment.getStatus(),
                clientSecret, payment.getCreatedAt());
    }
}
