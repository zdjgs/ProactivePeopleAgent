package com.proactiveperson.proactive.disturbance;

import com.proactiveperson.monitor.ActivityStatus;

import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * 防干扰决策：模式 + 临时静音 + 忙碌度 + 推送优先级。
 */
public final class DisturbancePolicy {

    private DisturbancePolicy() {
    }

    public static Decision evaluate(DisturbancePreference preference,
                                    PushPriority priority,
                                    ActivityStatus activity,
                                    ZonedDateTime nowInUserZone) {
        if (preference.mutedUntil() != null && nowInUserZone.toInstant().isBefore(preference.mutedUntil())) {
            return Decision.deny("MUTED_TODAY", "用户设置了「今天别打扰」，静音尚未结束");
        }

        if (activity == ActivityStatus.HIGH_INTENSITY && priority != PushPriority.HIGH) {
            return Decision.deny("BUSY", "检测到高强度工作，暂缓普通主动推送");
        }

        return switch (preference.mode()) {
            case NORMAL -> Decision.allow();
            case QUIET -> Decision.deny("QUIET_MODE", "完全静默模式，禁止主动推送");
            case IMPORTANT_ONLY -> priority == PushPriority.HIGH
                    ? Decision.allow()
                    : Decision.deny("IMPORTANT_ONLY", "仅重要模式，普通推送被拦截");
            case CUSTOM_HOURS -> evaluateCustomHours(preference, priority, nowInUserZone.toLocalTime());
        };
    }

    private static Decision evaluateCustomHours(DisturbancePreference preference,
                                                PushPriority priority,
                                                LocalTime localTime) {
        LocalTime start = preference.customQuietStart();
        LocalTime end = preference.customQuietEnd();
        if (start == null || end == null) {
            return Decision.deny("CUSTOM_HOURS_MISCONFIGURED", "自定义时段未配置起止时间");
        }
        boolean inQuiet = isInQuietWindow(localTime, start, end);
        if (!inQuiet) {
            return Decision.allow();
        }
        if (priority == PushPriority.HIGH) {
            return Decision.allow();
        }
        return Decision.deny("CUSTOM_QUIET_HOURS",
                "自定义勿扰时段 " + start + "-" + end + " 内仅允许高优先级推送");
    }

    /**
     * 支持跨午夜：如 22:00–08:00。
     */
    static boolean isInQuietWindow(LocalTime now, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        // 跨午夜：now >= start || now < end
        return !now.isBefore(start) || now.isBefore(end);
    }

    public record Decision(boolean allowed, String code, String reason) {

        public static Decision allow() {
            return new Decision(true, "OK", "ok");
        }

        public static Decision deny(String code, String reason) {
            return new Decision(false, code, reason);
        }
    }
}
