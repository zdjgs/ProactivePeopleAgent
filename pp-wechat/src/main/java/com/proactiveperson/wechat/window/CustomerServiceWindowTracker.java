package com.proactiveperson.wechat.window;

import com.proactiveperson.common.state.StateStore;
import com.proactiveperson.wechat.config.WeChatProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 客服消息 48h 互动窗口：以用户最近一次入站消息时间为起点。
 * 状态经 {@link StateStore} 外置，可切换 Redis 供多实例共享。
 */
@Service
public class CustomerServiceWindowTracker {

    private static final String KEY_PREFIX = "pp:wechat:cs-window:";

    private final WeChatProperties properties;
    private final StateStore stateStore;
    private final Clock clock;

    @Autowired
    public CustomerServiceWindowTracker(WeChatProperties properties, StateStore stateStore) {
        this(properties, stateStore, Clock.systemUTC());
    }

    public static CustomerServiceWindowTracker createForTest(WeChatProperties properties,
                                                             StateStore stateStore,
                                                             Clock clock) {
        return new CustomerServiceWindowTracker(properties, stateStore, clock);
    }

    private CustomerServiceWindowTracker(WeChatProperties properties, StateStore stateStore, Clock clock) {
        this.properties = properties;
        this.stateStore = stateStore;
        this.clock = clock;
    }

    public void recordInbound(String openId, Instant at) {
        if (openId == null || openId.isBlank() || at == null) {
            return;
        }
        String key = key(openId);
        Optional<Instant> existing = lastInbound(openId);
        if (existing.isPresent() && !at.isAfter(existing.get())) {
            return;
        }
        Duration ttl = Duration.ofHours(properties.getCustomerServiceWindowHours() + 1L);
        stateStore.set(key, at.toString(), ttl);
    }

    public Optional<Instant> lastInbound(String openId) {
        if (openId == null || openId.isBlank()) {
            return Optional.empty();
        }
        return stateStore.get(key(openId)).flatMap(this::parseInstant);
    }

    public boolean isWithinCustomerServiceWindow(String openId) {
        Instant last = lastInbound(openId).orElse(null);
        if (last == null) {
            return false;
        }
        Instant now = Instant.now(clock);
        Duration elapsed = Duration.between(last, now);
        Duration window = Duration.ofHours(properties.getCustomerServiceWindowHours());
        return !elapsed.isNegative() && elapsed.compareTo(window) <= 0;
    }

    public void clear() {
        // 无全量枚举；测试使用独立 InMemoryStateStore 实例即可
    }

    private Optional<Instant> parseInstant(String raw) {
        try {
            return Optional.of(Instant.parse(raw));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static String key(String openId) {
        return KEY_PREFIX + openId;
    }
}
