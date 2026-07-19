package com.proactiveperson.proactive.gate;

import com.proactiveperson.monitor.ActivityMonitor;
import com.proactiveperson.monitor.ActivityStatus;
import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.disturbance.DisturbanceMode;
import com.proactiveperson.proactive.disturbance.DisturbancePolicy;
import com.proactiveperson.proactive.disturbance.DisturbancePreference;
import com.proactiveperson.proactive.disturbance.DisturbancePreferenceService;
import com.proactiveperson.proactive.disturbance.PushPriority;
import com.proactiveperson.proactive.quota.DailyPushQuotaService;
import com.proactiveperson.proactive.user.ProactiveUser;
import com.proactiveperson.proactive.window.MorningPushWindowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;

/**
 * 早间推送规则门禁：时区窗口 + 防干扰策略 + 日上限。
 */
@Service
public class MorningPushGate {

    private final MorningPushWindowService windowService;
    private final DailyPushQuotaService quotaService;
    private final ProactiveProperties properties;
    private final DisturbancePreferenceService preferenceService;
    private final ActivityMonitor activityMonitor;
    private final Clock clock;

    @Autowired
    public MorningPushGate(MorningPushWindowService windowService,
                           DailyPushQuotaService quotaService,
                           ProactiveProperties properties,
                           DisturbancePreferenceService preferenceService,
                           ActivityMonitor activityMonitor) {
        this(windowService, quotaService, properties, preferenceService, activityMonitor, Clock.systemUTC());
    }

    public static MorningPushGate createForTest(MorningPushWindowService windowService,
                                                DailyPushQuotaService quotaService,
                                                ProactiveProperties properties,
                                                DisturbancePreferenceService preferenceService,
                                                ActivityMonitor activityMonitor,
                                                Clock clock) {
        return new MorningPushGate(windowService, quotaService, properties,
                preferenceService, activityMonitor, clock);
    }

    private MorningPushGate(MorningPushWindowService windowService,
                            DailyPushQuotaService quotaService,
                            ProactiveProperties properties,
                            DisturbancePreferenceService preferenceService,
                            ActivityMonitor activityMonitor,
                            Clock clock) {
        this.windowService = windowService;
        this.quotaService = quotaService;
        this.properties = properties;
        this.preferenceService = preferenceService;
        this.activityMonitor = activityMonitor;
        this.clock = clock;
    }

    public Decision evaluate(ProactiveUser user) {
        return evaluate(user, PushPriority.NORMAL);
    }

    public Decision evaluate(ProactiveUser user, PushPriority priority) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(user.timezone());

        if (!windowService.isInMorningWindow(user.timezone(), now)) {
            return Decision.deny("OUTSIDE_MORNING_WINDOW",
                    "不在用户本地 " + properties.getMorningWindowStartHour()
                            + "-" + properties.getMorningWindowEndHour() + " 窗口",
                    now);
        }

        DisturbancePreference preference = resolvePreference(user);
        ActivityStatus activity = activityMonitor.currentStatus(user.userId());
        DisturbancePolicy.Decision disturbance = DisturbancePolicy.evaluate(
                preference, priority, activity, now);
        if (!disturbance.allowed()) {
            return Decision.deny(disturbance.code(), disturbance.reason(), now);
        }

        if (!quotaService.hasMorningSlot(user, now)) {
            return Decision.deny("ALREADY_MORNING_TODAY",
                    "今日早间主动消息已推送，每日仅一次",
                    now);
        }
        if (!quotaService.canPush(user, now)) {
            return Decision.deny("DAILY_LIMIT",
                    "已达日主动推送上限 " + properties.getDailyPushLimit(),
                    now);
        }
        return Decision.allow(now);
    }

    /**
     * 配置侧 do-not-disturb=true 且尚无持久化偏好时，视为 QUIET（兼容 T-005）。
     */
    private DisturbancePreference resolvePreference(ProactiveUser user) {
        DisturbancePreference loaded = preferenceService.get(user.userId());
        if (user.doNotDisturb()
                && loaded.mode() == DisturbanceMode.NORMAL
                && loaded.mutedUntil() == null) {
            return DisturbancePreference.quiet();
        }
        return loaded;
    }

    public record Decision(boolean allowed, String code, String reason, ZonedDateTime nowInUserZone) {

        public static Decision allow(ZonedDateTime now) {
            return new Decision(true, "OK", "ok", now);
        }

        public static Decision deny(String code, String reason, ZonedDateTime now) {
            return new Decision(false, code, reason, now);
        }
    }
}
