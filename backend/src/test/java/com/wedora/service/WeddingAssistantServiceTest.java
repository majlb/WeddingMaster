package com.wedora.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedora.domain.AppUser;
import com.wedora.repository.SupplierRepository;
import com.wedora.repository.UserRepository;
import com.wedora.web.ApiException;
import com.wedora.web.dto.AssistantChatRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.InterruptedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeddingAssistantServiceTest {
    @Mock private UserRepository users;
    @Mock private SupplierRepository suppliers;
    @Mock private ChatLanguageModel chatModel;

    private WeddingAssistantService service;
    private AppUser couple;

    @BeforeEach
    void setUp() {
        service = new WeddingAssistantService(users, suppliers, new ObjectMapper(), chatModel);
        couple = new AppUser();
        couple.setEmail("couple@example.test");
        when(users.findByEmailIgnoreCase(couple.getEmail())).thenReturn(Optional.of(couple));
        when(suppliers.findByApprovedTrueAndActiveTrue()).thenReturn(List.of());
    }

    @Test
    void sendsConversationAndSupplierContextToTheConfiguredLocalModel() {
        when(chatModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("Try a garden venue.")));

        String reply = service.reply(couple.getEmail(),
                List.of(new AssistantChatRequest.Message("user", "Suggest a venue.")), Locale.ENGLISH);

        ArgumentCaptor<List<ChatMessage>> conversation = ArgumentCaptor.forClass(List.class);
        verify(chatModel).generate(conversation.capture());
        assertThat(reply).isEqualTo("Try a garden venue.");
        assertThat(conversation.getValue()).hasSize(2);
        assertThat(conversation.getValue().get(0)).isInstanceOf(SystemMessage.class);
        assertThat(conversation.getValue().get(1)).isInstanceOf(UserMessage.class);
        assertThat(((SystemMessage) conversation.getValue().get(0)).text()).contains("Approved supplier directory");
    }

    @Test
    void reportsWhenTheLocalModelCannotBeReached() {
        when(chatModel.generate(anyList())).thenThrow(new IllegalStateException("connection refused"));

        assertThatThrownBy(() -> service.reply(couple.getEmail(),
                List.of(new AssistantChatRequest.Message("user", "Hello")), Locale.ENGLISH))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("assistant.local_model_unavailable");
                });
    }

    @Test
    void reportsWhenTheLocalModelTimesOut() {
        when(chatModel.generate(anyList())).thenThrow(new RuntimeException(new InterruptedIOException("timeout")));

        assertThatThrownBy(() -> service.reply(couple.getEmail(),
                List.of(new AssistantChatRequest.Message("user", "Hello")), Locale.ENGLISH))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                    assertThat(apiException.getCode()).isEqualTo("assistant.local_model_timeout");
                });
    }
}
