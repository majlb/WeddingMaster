package com.wedora.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AssistantConfiguration {
    @Bean
    ChatLanguageModel weddingAssistantChatModel(
            @Value("${app.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.ai.ollama.model:llama3.2:3b}") String model,
            @Value("${app.ai.ollama.timeout-seconds:180}") long timeoutSeconds) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.6)
                .numPredict(500)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(0)
                .build();
    }
}
