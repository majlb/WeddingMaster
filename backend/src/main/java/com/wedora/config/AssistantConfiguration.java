package com.wedora.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Locale;

@Configuration
public class AssistantConfiguration {
    @Bean
    ChatLanguageModel weddingAssistantChatModel(
            @Value("${app.ai.provider:ollama}") String provider,
            @Value("${app.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.ai.ollama.model:llama3.2:3b}") String model,
            @Value("${app.ai.ollama.timeout-seconds:180}") long ollamaTimeoutSeconds,
            @Value("${app.ai.openrouter.base-url:https://openrouter.ai/api/v1}") String openRouterBaseUrl,
            @Value("${app.ai.openrouter.model:openai/gpt-4o-mini}") String openRouterModel,
            @Value("${app.ai.openrouter.api-key:}") String openRouterApiKey,
            @Value("${app.ai.openrouter.timeout-seconds:60}") long openRouterTimeoutSeconds) {
        return switch (provider.trim().toLowerCase(Locale.ROOT)) {
            case "ollama" -> OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(model)
                    .temperature(0.6)
                    .numPredict(500)
                    .timeout(Duration.ofSeconds(ollamaTimeoutSeconds))
                    .maxRetries(0)
                    .build();
            case "openrouter" -> {
                if (openRouterApiKey.isBlank()) {
                    throw new IllegalStateException(
                            "OPENROUTER_API_KEY is required when AI_PROVIDER=openrouter");
                }
                yield OpenAiChatModel.builder()
                        .baseUrl(openRouterBaseUrl)
                        .apiKey(openRouterApiKey)
                        .modelName(openRouterModel)
                        .temperature(0.6)
                        .maxTokens(500)
                        .timeout(Duration.ofSeconds(openRouterTimeoutSeconds))
                        .maxRetries(0)
                        .build();
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported AI_PROVIDER: " + provider + ". Supported values are ollama and openrouter.");
        };
    }
}
