package com.proactiveperson.proactive.disturbance.cache;

import com.proactiveperson.proactive.config.DisturbanceProperties;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 缓存实现（pp.disturbance.cache-provider=redis）。
 * 使用 Lettuce 直连，避免未启用 Redis 时 Spring 自动配置拖垮启动。
 */
@Component
@ConditionalOnProperty(name = "pp.disturbance.cache-provider", havingValue = "redis")
public class RedisPreferenceCache implements PreferenceCache {

    private static final Logger log = LoggerFactory.getLogger(RedisPreferenceCache.class);

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;

    public RedisPreferenceCache(DisturbanceProperties properties) {
        String uri = properties.getRedisUri();
        this.client = RedisClient.create(uri);
        this.connection = client.connect();
        this.commands = connection.sync();
        log.info("RedisPreferenceCache connected uri={}", uri);
    }

    @Override
    public Optional<String> get(String key) {
        String value = commands.get(key);
        return Optional.ofNullable(value);
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        commands.setex(key, Math.max(1, ttl.toSeconds()), value);
    }

    @Override
    public void evict(String key) {
        commands.del(key);
    }

    @PreDestroy
    void close() {
        connection.close();
        client.shutdown();
    }
}
