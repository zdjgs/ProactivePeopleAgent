package com.proactiveperson.common.state;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StateStoreProperties.class)
public class StateStoreConfiguration {

    @Bean
    @ConditionalOnProperty(name = "pp.state.store", havingValue = "memory", matchIfMissing = true)
    @ConditionalOnMissingBean(StateStore.class)
    StateStore inMemoryStateStore() {
        return new InMemoryStateStore();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "pp.state.store", havingValue = "redis")
    @ConditionalOnMissingBean(StateStore.class)
    StateStore redisStateStore(StateStoreProperties properties) {
        return new RedisStateStore(properties.getRedisUri());
    }
}
