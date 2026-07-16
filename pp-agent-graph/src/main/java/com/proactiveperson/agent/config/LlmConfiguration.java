package com.proactiveperson.agent.config;

import com.proactiveperson.agent.Assistant;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AgentGraphProperties.class)
public class LlmConfiguration {

    @Bean
    @ConditionalOnProperty(name = "pp.llm.enabled", havingValue = "true")
    ChatModel chatModel(AgentGraphProperties properties) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("pp.llm.enabled=true 但未配置 pp.llm.api-key / OPENAI_API_KEY");
        }
        return OpenAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getModelName())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "pp.llm.enabled", havingValue = "true")
    Assistant assistant(ChatModel chatModel) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .build();
    }
}
