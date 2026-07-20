package com.proactiveperson.proactive.task;

import com.proactiveperson.monitor.ActivityMonitor;
import com.proactiveperson.monitor.ActivityStatus;
import com.proactiveperson.proactive.disturbance.DisturbancePolicy;
import com.proactiveperson.proactive.disturbance.DisturbancePreference;
import com.proactiveperson.proactive.disturbance.DisturbancePreferenceService;
import com.proactiveperson.proactive.disturbance.PushPriority;
import com.proactiveperson.proactive.quota.DailyPushQuotaService;
import com.proactiveperson.proactive.user.ProactiveUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;

/**
 * 任务跟进门禁：防干扰 + 忙碌度 + 日配额（不含早间窗口）。
 */
@Service
public class TaskFollowUpGate {

    private final DisturbancePreferenceService preferenceService;
    private final ActivityMonitor activityMonitor;
    private final DailyPushQuotaService quotaService;
    private final Clock clock;

    @Autowired
    public TaskFollowUpGate(DisturbancePreferenceService preferenceService,
                            ActivityMonitor activityMonitor,
                            DailyPushQuotaService quotaService) {
        this(preferenceService, activityMonitor, quotaService, Clock.systemUTC());
    }

    public static TaskFollowUpGate createForTest(DisturbancePreferenceService preferenceService,
                                                 ActivityMonitor activityMonitor,
                                                 DailyPushQuotaService quotaService,
                                                 Clock clock) {
        return new TaskFollowUpGate(preferenceService, activityMonitor, quotaService, clock);
    }

    private TaskFollowUpGate(DisturbancePreferenceService preferenceService,
                             ActivityMonitor activityMonitor,
                             DailyPushQuotaService quotaService,
                             Clock clock) {
        this.preferenceService = preferenceService;
        this.activityMonitor = activityMonitor;
        this.quotaService = quotaService;
        this.clock = clock;
    }

    public Decision evaluate(ProactiveUser user, PushPriority priority) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(user.timezone());
        DisturbancePreference preference = preferenceService.get(user.userId());
        ActivityStatus activity = activityMonitor.currentStatus(user.userId());
        DisturbancePolicy.Decision disturbance = DisturbancePolicy.evaluate(
                preference, priority, activity, now);
        if (!disturbance.allowed()) {
            return Decision.deny(disturbance.code(), disturbance.reason(), now, priority);
        }
        if (!quotaService.canPush(user, now)) {
            return Decision.deny("DAILY_LIMIT", "已达日主动推送上限", now, priority);
        }
        return Decision.allow(now, priority);
    }

    public record Decision(boolean allowed, String code, String reason,
                           ZonedDateTime nowInUserZone, PushPriority priority) {

        static Decision allow(ZonedDateTime now, PushPriority priority) {
            return new Decision(true, "OK", "ok", now, priority);
        }

        static Decision deny(String code, String reason, ZonedDateTime now, PushPriority priority) {
            return new Decision(false, code, reason, now, priority);
        }
    }
}
