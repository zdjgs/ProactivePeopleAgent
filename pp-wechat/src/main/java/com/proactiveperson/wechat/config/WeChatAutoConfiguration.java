package com.proactiveperson.wechat.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WeChatProperties.class)
@ComponentScan(basePackages = "com.proactiveperson.wechat")
public class WeChatAutoConfiguration {
}
