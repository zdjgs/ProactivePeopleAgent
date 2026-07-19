package com.proactiveperson.proactive.quota;

import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.user.ProactiveUser;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 按用户本地日期统计主动推送次数，执行日上限（默认 ≤2）。
 */
@Service
public class DailyPushQuotaService {

    private final ProactiveProperties properties;
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public DailyPushQuotaService(ProactiveProperties properties) {
        this.properties = properties;
    }

    public boolean canPush(ProactiveUser user, ZonedDateTime nowInUserZone) {
        return currentCount(user, nowInUserZone) < properties.getDailyPushLimit();
    }

    public int currentCount(ProactiveUser user, ZonedDateTime nowInUserZone) {
        return counters.getOrDefault(key(user, nowInUserZone.toLocalDate()), new AtomicInteger(0)).get();
    }

    public void recordPush(ProactiveUser user, ZonedDateTime nowInUserZone) {
        counters.computeIfAbsent(key(user, nowInUserZone.toLocalDate()), k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public void clear() {
        counters.clear();
    }

    private static String key(ProactiveUser user, LocalDate localDate) {
        return user.userId() + ":" + localDate;
    }
}
