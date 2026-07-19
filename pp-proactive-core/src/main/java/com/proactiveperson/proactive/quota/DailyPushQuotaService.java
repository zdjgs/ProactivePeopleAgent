package com.proactiveperson.proactive.quota;

import com.proactiveperson.common.state.StateStore;
import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.user.ProactiveUser;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * 主动推送配额：早间每日一次 + 全日上限预占（默认 ≤2），状态可落 Redis 供多实例共享。
 */
@Service
public class DailyPushQuotaService {

    public enum ReserveResult {
        RESERVED,
        ALREADY_MORNING_TODAY,
        DAILY_LIMIT
    }

    private final ProactiveProperties properties;
    private final StateStore stateStore;

    public DailyPushQuotaService(ProactiveProperties properties, StateStore stateStore) {
        this.properties = properties;
        this.stateStore = stateStore;
    }

    public boolean canPush(ProactiveUser user, ZonedDateTime nowInUserZone) {
        return currentCount(user, nowInUserZone) < properties.getDailyPushLimit();
    }

    public boolean hasMorningSlot(ProactiveUser user, ZonedDateTime nowInUserZone) {
        return stateStore.get(morningKey(user, nowInUserZone.toLocalDate())).isEmpty();
    }

    public int currentCount(ProactiveUser user, ZonedDateTime nowInUserZone) {
        return stateStore.get(quotaKey(user, nowInUserZone.toLocalDate()))
                .map(v -> {
                    try {
                        return Integer.parseInt(v);
                    } catch (NumberFormatException ex) {
                        return 0;
                    }
                })
                .orElse(0);
    }

    /**
     * 原子预占：先占早间槽位，再 INCR 日配额；失败时回滚。
     */
    public ReserveResult tryReserveMorning(ProactiveUser user, ZonedDateTime nowInUserZone) {
        LocalDate date = nowInUserZone.toLocalDate();
        Duration ttl = ttlForLocalDate(date);
        String morning = morningKey(user, date);
        String quota = quotaKey(user, date);

        if (!stateStore.setIfAbsent(morning, "1", ttl)) {
            return ReserveResult.ALREADY_MORNING_TODAY;
        }

        long count = stateStore.increment(quota, ttl);
        if (count > properties.getDailyPushLimit()) {
            stateStore.decrement(quota);
            stateStore.delete(morning);
            return ReserveResult.DAILY_LIMIT;
        }
        return ReserveResult.RESERVED;
    }

    /** 发送失败时释放预占，允许窗口内重试。 */
    public void releaseMorning(ProactiveUser user, ZonedDateTime nowInUserZone) {
        LocalDate date = nowInUserZone.toLocalDate();
        stateStore.delete(morningKey(user, date));
        stateStore.decrement(quotaKey(user, date));
    }

    /** 测试辅助：直接记一次成功推送（不占早间槽）。 */
    public void recordPush(ProactiveUser user, ZonedDateTime nowInUserZone) {
        stateStore.increment(quotaKey(user, nowInUserZone.toLocalDate()), ttlForLocalDate(nowInUserZone.toLocalDate()));
    }

    public void clear() {
        // 仅内存实现支持全清；Redis 由 TTL 自然过期
        if (stateStore instanceof com.proactiveperson.common.state.InMemoryStateStore memory) {
            memory.clear();
        }
    }

    private static Duration ttlForLocalDate(LocalDate date) {
        // 覆盖跨时区边界，保留两天足够日切
        return Duration.ofDays(2);
    }

    private static String morningKey(ProactiveUser user, LocalDate localDate) {
        return "pp:morning:" + user.userId() + ":" + localDate;
    }

    private static String quotaKey(ProactiveUser user, LocalDate localDate) {
        return "pp:quota:" + user.userId() + ":" + localDate;
    }
}
