package com.proactiveperson.wechat.window;

import com.proactiveperson.wechat.config.WeChatProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客服消息 48h 互动窗口：以用户最近一次入站消息时间为起点。
 * <p>
 * 进程内存储，后续可替换为 Redis（与防干扰偏好缓存同一路径）。
 */
@Service
public class CustomerServiceWindowTracker {

    private final WeChatProperties properties;
    private final Clock clock;
    private final Map<String, Instant> lastInboundAt = new ConcurrentHashMap<>();

    @Autowired
    public CustomerServiceWindowTracker(WeChatProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public static CustomerServiceWindowTracker createForTest(WeChatProperties properties, Clock clock) {
        return new CustomerServiceWindowTracker(properties, clock);
    }

    private CustomerServiceWindowTracker(WeChatProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void recordInbound(String openId, Instant at) {
        if (openId == null || openId.isBlank() || at == null) {
            return;
        }
        lastInboundAt.merge(openId, at, (old, neu) -> neu.isAfter(old) ? neu : old);
    }

    public Optional<Instant> lastInbound(String openId) {
        return Optional.ofNullable(lastInboundAt.get(openId));
    }

    public boolean isWithinCustomerServiceWindow(String openId) {
        Instant last = lastInboundAt.get(openId);
        if (last == null) {
            return false;
        }
        Instant now = Instant.now(clock);
        Duration elapsed = Duration.between(last, now);
        Duration window = Duration.ofHours(properties.getCustomerServiceWindowHours());
        return !elapsed.isNegative() && elapsed.compareTo(window) <= 0;
    }

    public void clear() {
        lastInboundAt.clear();
    }
}
