package com.wedora.service;

import com.wedora.domain.AppUser;
import com.wedora.domain.Message;
import com.wedora.repository.MessageRepository;
import com.wedora.repository.UserRepository;
import com.wedora.web.ApiException;
import com.wedora.web.dto.ChatMessageRequest;
import com.wedora.web.dto.MessageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ChatService {
    private final MessageRepository messages;
    private final UserRepository users;
    private final BookingService bookings;
    private final ChatAttachmentService attachments;
    private final UserNotificationService notifications;

    public ChatService(MessageRepository messages, UserRepository users, BookingService bookings,
                       ChatAttachmentService attachments, UserNotificationService notifications) {
        this.messages = messages;
        this.users = users;
        this.bookings = bookings;
        this.attachments = attachments;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> history(Long bookingId, String email) {
        bookings.requireParticipant(bookingId, email);
        return messages.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(MessageResponse::from).toList();
    }

    @Transactional
    public MessageResponse send(Long bookingId, String email, ChatMessageRequest request) {
        var booking = bookings.requireParticipant(bookingId, email);
        if ((request.body() == null || request.body().isBlank())
                && request.attachmentId() == null) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Message text or an attachment is required");
        }
        AppUser sender = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
        Message message = new Message();
        message.setBooking(booking);
        message.setSender(sender);
        message.setBody(request.body());
        if (request.attachmentId() != null) {
            attachments.requireForBooking(bookingId, request.attachmentId());
            message.setAttachmentUrl("/bookings/" + bookingId + "/attachments/" + request.attachmentId());
        }
        Message saved = messages.save(message);
        AppUser recipient = sender.getId().equals(booking.getCustomer().getId())
                ? booking.getSupplier().getOwner()
                : booking.getCustomer();
        notifications.notify(recipient, booking, "NEW_MESSAGE", sender.getName());
        return MessageResponse.from(saved);
    }
}
