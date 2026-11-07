package com.wedora.web.dto;

import com.wedora.domain.UserNotification;

import java.time.Instant;
import java.time.LocalDate;

public record UserNotificationResponse(
        Long id,
        Long bookingId,
        Long collaborationId,
        String type,
        String actorName,
        LocalDate eventDate,
        Instant createdAt,
        boolean read
) {
    public static UserNotificationResponse from(UserNotification notification) {
        return new UserNotificationResponse(
                notification.getId(),
                notification.getBooking() == null ? null : notification.getBooking().getId(),
                notification.getCollaborationId(),
                notification.getType(),
                notification.getActorName(),
                notification.getBooking() == null ? null : notification.getBooking().getEventDate(),
                notification.getCreatedAt(),
                notification.getReadAt() != null
        );
    }
}
