package com.proactiveperson.agent.config;

import com.proactiveperson.agent.Assistant;
import com.proactiveperson.common.util.SensitiveDataMasker;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AgentGraphProperties.class)
public class LlmConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LlmConfiguration.class);

    @Bean
    @ConditionalOnProperty(name = "pp.llm.enabled", havingValue = "true")
    ChatModel chatModel(AgentGraphProperties properties) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("pp.llm.enabled=true 但未配置 pp.llm.api-key / OPENAI_API_KEY");
        }
        log.info("init ChatModel baseUrl={} model={} temperature={} timeout={}s apiKey={}",
                properties.getBaseUrl(),
                properties.getModelName(),
                properties.getTemperature(),
                properties.getTimeoutSeconds(),
                SensitiveDataMasker.maskSecret(properties.getApiKey()));

        return OpenAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getModelName())
                .temperature(properties.getTemperature())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "pp.llm.enabled", havingValue = "true")
    ChatMemoryProvider chatMemoryProvider(AgentGraphProperties properties) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(properties.getChatMemoryMaxMessages())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "pp.llm.enabled", havingValue = "true")
    Assistant assistant(ChatModel chatModel, ChatMemoryProvider chatMemoryProvider) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }
}
