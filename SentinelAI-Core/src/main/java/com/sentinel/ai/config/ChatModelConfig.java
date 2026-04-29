package com.sentinel.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfig {
    /**
     * Spring AI auto-configures a {@link ChatModel} from properties in {@code application.yaml}:
     * - {@code spring.ai.ollama.base-url}
     * - {@code spring.ai.ollama.chat.model}
     *
     * This bean adapts that {@link ChatModel} into the higher-level {@link ChatClient} API,
     * which provides a fluent interface for prompting and reading the response content.
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
