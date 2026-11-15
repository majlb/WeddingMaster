package com.wedora.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

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
}
