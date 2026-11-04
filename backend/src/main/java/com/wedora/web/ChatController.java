package com.wedora.web;

import com.wedora.service.ChatService;
import com.wedora.web.dto.ChatMessageRequest;
import com.wedora.web.dto.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/bookings/{bookingId}/messages")
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping
    public List<MessageResponse> history(@PathVariable Long bookingId, @AuthenticationPrincipal String email) {
        return chatService.history(bookingId, email);
    }

    @MessageMapping("/bookings/{bookingId}")
    public void send(@DestinationVariable Long bookingId, @Valid @Payload ChatMessageRequest request, Principal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Authentication is required");
        MessageResponse message = chatService.send(bookingId, principal.getName(), request);
        messagingTemplate.convertAndSend("/topic/bookings/" + bookingId, message);
    }
}
