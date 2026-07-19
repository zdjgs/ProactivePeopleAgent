package com.proactiveperson;

import com.proactiveperson.agent.config.AgentGraphProperties;
import com.proactiveperson.app.config.ApiSecurityProperties;
import com.proactiveperson.app.config.InfraProperties;
import com.proactiveperson.common.state.StateStoreProperties;
import com.proactiveperson.memory.config.MemoryProperties;
import com.proactiveperson.proactive.config.DisturbanceProperties;
import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.wechat.config.WeChatProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.proactiveperson")
@EnableConfigurationProperties({
        AgentGraphProperties.class,
        ProactiveProperties.class,
        InfraProperties.class,
        MemoryProperties.class,
        WeChatProperties.class,
        StateStoreProperties.class,
        ApiSecurityProperties.class,
        DisturbanceProperties.class
})
public class ProactivePersonApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProactivePersonApplication.class, args);
    }
}
