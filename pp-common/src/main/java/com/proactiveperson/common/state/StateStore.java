package com.proactiveperson.common.state;

import java.time.Duration;
import java.util.Optional;

/**
 * 进程内 / Redis 可切换的轻量状态存储，供配额预占与微信 48h 窗口等跨实例状态使用。
 */
public interface StateStore {

    Optional<String> get(String key);

    void set(String key, String value, Duration ttl);

    /** @return true 表示本次写入成功（键原先不存在或已过期） */
    boolean setIfAbsent(String key, String value, Duration ttl);

    /** 原子自增；键不存在时从 1 开始并设置 TTL */
    long increment(String key, Duration ttlOnCreate);

    /** 原子自减；≤0 时删除键；键不存在返回 0 */
    long decrement(String key);

    void delete(String key);
}
