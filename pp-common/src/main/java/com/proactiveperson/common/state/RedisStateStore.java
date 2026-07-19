package com.proactiveperson.common.state;

import io.lettuce.core.RedisClient;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

/**
 * Lettuce 直连 Redis 的 StateStore，避免未启用时 Spring Data Redis 自动配置拖垮启动。
 */
public class RedisStateStore implements StateStore {

    private static final Logger log = LoggerFactory.getLogger(RedisStateStore.class);

    private static final String INCR_WITH_EXPIRE = """
            local v = redis.call('INCR', KEYS[1])
            if v == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return v
            """;

    private static final String DECR_OR_DEL = """
            if redis.call('EXISTS', KEYS[1]) == 0 then
              return 0
            end
            local v = redis.call('DECR', KEYS[1])
            if v <= 0 then
              redis.call('DEL', KEYS[1])
              return 0
            end
            return v
            """;

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;

    public RedisStateStore(String redisUri) {
        this.client = RedisClient.create(redisUri);
        this.connection = client.connect();
        this.commands = connection.sync();
        log.info("RedisStateStore connected uri={}", redisUri);
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(commands.get(key));
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        commands.setex(key, ttlSeconds(ttl), value);
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        String result = commands.set(key, value, SetArgs.Builder.nx().ex(ttlSeconds(ttl)));
        return "OK".equalsIgnoreCase(result);
    }

    @Override
    public long increment(String key, Duration ttlOnCreate) {
        Long value = commands.eval(INCR_WITH_EXPIRE, io.lettuce.core.ScriptOutputType.INTEGER,
                new String[]{key}, String.valueOf(ttlSeconds(ttlOnCreate)));
        return value == null ? 0L : value;
    }

    @Override
    public long decrement(String key) {
        Long value = commands.eval(DECR_OR_DEL, io.lettuce.core.ScriptOutputType.INTEGER, new String[]{key});
        return value == null ? 0L : value;
    }

    @Override
    public void delete(String key) {
        commands.del(key);
    }

    public void close() {
        connection.close();
        client.shutdown();
    }

    private static long ttlSeconds(Duration ttl) {
        return Math.max(1, ttl.toSeconds());
    }
}
