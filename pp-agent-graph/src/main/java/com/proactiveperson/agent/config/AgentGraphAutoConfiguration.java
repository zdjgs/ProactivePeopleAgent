package com.proactiveperson.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(LlmConfiguration.class)
public class AgentGraphAutoConfiguration {
}
