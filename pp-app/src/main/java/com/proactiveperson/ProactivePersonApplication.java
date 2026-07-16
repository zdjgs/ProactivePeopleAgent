package com.proactiveperson;

import com.proactiveperson.agent.config.AgentGraphProperties;
import com.proactiveperson.app.config.InfraProperties;
import com.proactiveperson.proactive.config.ProactiveProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.proactiveperson")
@EnableConfigurationProperties({AgentGraphProperties.class, ProactiveProperties.class, InfraProperties.class})
public class ProactivePersonApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProactivePersonApplication.class, args);
    }
}
