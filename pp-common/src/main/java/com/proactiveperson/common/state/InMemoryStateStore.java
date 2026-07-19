package com.proactiveperson.common.state;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 本地并发安全 StateStore，适合单机与单测；多实例请切换 Redis。
 */
public class InMemoryStateStore implements StateStore {

    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String key) {
        Entry entry = map.get(key);
        if (entry == null || entry.expired()) {
            if (entry != null) {
                map.remove(key, entry);
            }
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        map.put(key, Entry.of(value, ttl));
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        AtomicBoolean inserted = new AtomicBoolean(false);
        map.compute(key, (k, old) -> {
            if (old != null && !old.expired()) {
                return old;
            }
            inserted.set(true);
            return Entry.of(value, ttl);
        });
        return inserted.get();
    }

    @Override
    public long increment(String key, Duration ttlOnCreate) {
        Entry updated = map.compute(key, (k, old) -> {
            if (old == null || old.expired()) {
                return Entry.of("1", ttlOnCreate);
            }
            long next = Long.parseLong(old.value()) + 1;
            return new Entry(String.valueOf(next), old.expireAtMs());
        });
        return Long.parseLong(updated.value());
    }

    @Override
    public long decrement(String key) {
        Entry[] holder = new Entry[1];
        map.compute(key, (k, old) -> {
            if (old == null || old.expired()) {
                holder[0] = null;
                return null;
            }
            long next = Long.parseLong(old.value()) - 1;
            if (next <= 0) {
                holder[0] = new Entry("0", old.expireAtMs());
                return null;
            }
            Entry entry = new Entry(String.valueOf(next), old.expireAtMs());
            holder[0] = entry;
            return entry;
        });
        return holder[0] == null ? 0L : Long.parseLong(holder[0].value());
    }

    @Override
    public void delete(String key) {
        map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    private record Entry(String value, long expireAtMs) {
        static Entry of(String value, Duration ttl) {
            long ms = Math.max(1, ttl.toMillis());
            return new Entry(value, System.currentTimeMillis() + ms);
        }

        boolean expired() {
            return System.currentTimeMillis() >= expireAtMs;
        }
    }
}
