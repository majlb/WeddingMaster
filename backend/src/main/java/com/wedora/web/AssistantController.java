package com.wedora.web;

import com.wedora.service.WeddingAssistantService;
import com.wedora.web.dto.AssistantChatRequest;
import com.wedora.web.dto.AssistantChatResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final WeddingAssistantService assistant;

    public AssistantController(WeddingAssistantService assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/chat")
    public AssistantChatResponse chat(@AuthenticationPrincipal String email,
                                      @Valid @RequestBody AssistantChatRequest request,
                                      Locale locale) {
        return new AssistantChatResponse(assistant.reply(email, request.messages(), locale));
    }
}
