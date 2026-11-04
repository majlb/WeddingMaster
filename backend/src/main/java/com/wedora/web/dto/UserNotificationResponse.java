package com.wedora.web.dto;

import com.wedora.domain.UserNotification;

import java.time.Instant;
import java.time.LocalDate;

public record UserNotificationResponse(
        Long id,
        Long bookingId,
        String type,
        String actorName,
        LocalDate eventDate,
        Instant createdAt,
        boolean read
) {
    public static UserNotificationResponse from(UserNotification notification) {
        return new UserNotificationResponse(
                notification.getId(),
                notification.getBooking().getId(),
                notification.getType(),
                notification.getActorName(),
                notification.getBooking().getEventDate(),
                notification.getCreatedAt(),
                notification.getReadAt() != null
        );
    }
}
