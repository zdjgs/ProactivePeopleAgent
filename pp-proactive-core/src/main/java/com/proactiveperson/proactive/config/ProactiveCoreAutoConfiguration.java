package com.proactiveperson.proactive.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ProactiveProperties.class)
@ComponentScan(basePackages = "com.proactiveperson.proactive")
public class ProactiveCoreAutoConfiguration {
}
