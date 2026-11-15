package com.wedora.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.ai4j.openai4j.OpenAiHttpException;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssistantConfigurationTest {
    private final AssistantConfiguration configuration = new AssistantConfiguration();

    @Test
    void createsOpenRouterModelWhenConfigured() {
        ChatLanguageModel model = configuration.weddingAssistantChatModel(
                "openrouter", "http://localhost:11434", "llama3.2:3b", 180,
                "https://openrouter.ai/api/v1", "openai/gpt-4o-mini", "test-key", 60);

        assertThat(model).isInstanceOf(OpenAiChatModel.class);
    }

    @Test
    void requiresAnApiKeyWhenOpenRouterIsSelected() {
        assertThatThrownBy(() -> configuration.weddingAssistantChatModel(
                "openrouter", "http://localhost:11434", "llama3.2:3b", 180,
                "https://openrouter.ai/api/v1", "openai/gpt-4o-mini", " ", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPENROUTER_API_KEY");
    }

    @Test
    void sendsOpenRouterCompatibleRequestsAndReadsReplies() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = ("{\"id\":\"test\",\"object\":\"chat.completion\",\"created\":1,"
                    + "\"model\":\"openai/gpt-4o-mini\",\"choices\":[{\"index\":0,"
                    + "\"message\":{\"role\":\"assistant\",\"content\":\"Hello\"},"
                    + "\"finish_reason\":\"stop\"}]}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var body = exchange.getResponseBody()) {
                body.write(response);
            }
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1";
            ChatLanguageModel model = configuration.weddingAssistantChatModel(
                    "openrouter", "http://localhost:11434", "llama3.2:3b", 180,
                    baseUrl, "openai/gpt-4o-mini", "test-key", 60);
            assertThat(model.generate(List.of(UserMessage.from("Hello"))).content().text()).isEqualTo("Hello");
            JsonNode request = new ObjectMapper().readTree(requestBody.get());
            assertThat(request.path("model").asText()).isEqualTo("openai/gpt-4o-mini");
            assertThat(request.path("messages").get(0).path("content").asText()).isEqualTo("Hello");
            assertThat(authorization.get()).isEqualTo("Bearer test-key");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void preservesProviderHttpStatusForDiagnostics() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/chat/completions", exchange -> {
            byte[] response = "{\"error\":{\"message\":\"Unauthorized\",\"code\":401}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, response.length);
            try (var body = exchange.getResponseBody()) {
                body.write(response);
            }
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1";
            ChatLanguageModel model = configuration.weddingAssistantChatModel(
                    "openrouter", "http://localhost:11434", "llama3.2:3b", 180,
                    baseUrl, "openai/gpt-4o-mini", "test-key", 60);
            assertThatThrownBy(() -> model.generate(List.of(UserMessage.from("Hello"))))
                    .hasCauseInstanceOf(OpenAiHttpException.class)
                    .satisfies(error -> assertThat(((OpenAiHttpException) error.getCause()).code()).isEqualTo(401));
        } finally {
            server.stop(0);
        }
    }
}
