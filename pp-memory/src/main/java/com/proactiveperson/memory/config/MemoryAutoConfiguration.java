package com.proactiveperson.memory.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MemoryProperties.class)
@ComponentScan(basePackages = "com.proactiveperson.memory")
public class MemoryAutoConfiguration {
}
