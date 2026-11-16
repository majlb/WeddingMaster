package com.wedora.web.dto;

import com.wedora.domain.BookingStatus;
import java.time.LocalDate;

public record ServiceBookingResponse(
        Long id, Long serviceOfferingId, String customerName, LocalDate eventDate,
        BookingStatus status, boolean dateBlocked
) {}
