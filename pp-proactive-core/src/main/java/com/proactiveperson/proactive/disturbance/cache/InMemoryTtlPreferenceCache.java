package com.proactiveperson.proactive.disturbance.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内 TTL 缓存（默认）。语义对齐 Redis SETEX，便于本地与测试。
 */
@Component
@ConditionalOnProperty(name = "pp.disturbance.cache-provider", havingValue = "memory", matchIfMissing = true)
public class InMemoryTtlPreferenceCache implements PreferenceCache {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        store.put(key, new Entry(value, Instant.now().plus(ttl)));
    }

    @Override
    public void evict(String key) {
        store.remove(key);
    }

    void clear() {
        store.clear();
    }

    private record Entry(String value, Instant expiresAt) {
    }
}
