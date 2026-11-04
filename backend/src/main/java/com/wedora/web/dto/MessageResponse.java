package com.wedora.web.dto;

import com.wedora.domain.Message;
import java.time.Instant;

public record MessageResponse(Long id, Long bookingId, Long senderId, String senderName,
                              String body, String attachmentUrl, Instant createdAt) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(message.getId(), message.getBooking().getId(),
                message.getSender().getId(), message.getSender().getName(), message.getBody(),
                message.getAttachmentUrl(), message.getCreatedAt());
    }
}
