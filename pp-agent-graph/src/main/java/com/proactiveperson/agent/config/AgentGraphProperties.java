package com.proactiveperson.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pp.llm")
public class AgentGraphProperties {

    /** 未配置 API Key 时保持 false，应用仍可启动 */
    private boolean enabled = false;

    private String apiKey = "";

    /** 兼容 OpenAI 协议的网关地址 */
    private String baseUrl = "https://api.openai.com/v1";

    private String modelName = "gpt-4o-mini";

    private double temperature = 0.7;

    private int timeoutSeconds = 60;

    /** 单会话保留的最大消息条数（user+assistant 合计） */
    private int chatMemoryMaxMessages = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getChatMemoryMaxMessages() {
        return chatMemoryMaxMessages;
    }

    public void setChatMemoryMaxMessages(int chatMemoryMaxMessages) {
        this.chatMemoryMaxMessages = chatMemoryMaxMessages;
    }
}
