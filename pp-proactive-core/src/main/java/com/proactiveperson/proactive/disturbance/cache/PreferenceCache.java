package com.proactiveperson.proactive.disturbance.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * 防干扰偏好缓存（Redis 或进程内 TTL，降低每次读 Mem0 的延迟）。
 */
public interface PreferenceCache {

    Optional<String> get(String key);

    void put(String key, String value, Duration ttl);

    void evict(String key);
}
