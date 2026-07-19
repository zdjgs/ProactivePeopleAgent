package com.proactiveperson.proactive.gate;

import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.quota.DailyPushQuotaService;
import com.proactiveperson.proactive.user.ProactiveUser;
import com.proactiveperson.proactive.window.MorningPushWindowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;

/**
 * 早间推送规则门禁：时区窗口 + 勿扰 + 日上限。
 * 完整三模式防干扰见 T-006；动态 JSON 规则见 T-011。
 */
@Service
public class MorningPushGate {

    private final MorningPushWindowService windowService;
    private final DailyPushQuotaService quotaService;
    private final ProactiveProperties properties;
    private final Clock clock;

    @Autowired
    public MorningPushGate(MorningPushWindowService windowService,
                           DailyPushQuotaService quotaService,
                           ProactiveProperties properties) {
        this(windowService, quotaService, properties, Clock.systemUTC());
    }

    public static MorningPushGate createForTest(MorningPushWindowService windowService,
                                                DailyPushQuotaService quotaService,
                                                ProactiveProperties properties,
                                                Clock clock) {
        return new MorningPushGate(windowService, quotaService, properties, clock);
    }

    private MorningPushGate(MorningPushWindowService windowService,
                            DailyPushQuotaService quotaService,
                            ProactiveProperties properties,
                            Clock clock) {
        this.windowService = windowService;
        this.quotaService = quotaService;
        this.properties = properties;
        this.clock = clock;
    }

    public Decision evaluate(ProactiveUser user) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(user.timezone());

        if (!windowService.isInMorningWindow(user.timezone(), now)) {
            return Decision.deny("OUTSIDE_MORNING_WINDOW",
                    "不在用户本地 " + properties.getMorningWindowStartHour()
                            + "-" + properties.getMorningWindowEndHour() + " 窗口",
                    now);
        }
        if (user.doNotDisturb()) {
            return Decision.deny("DO_NOT_DISTURB", "用户开启勿扰，跳过主动推送", now);
        }
        if (!quotaService.canPush(user, now)) {
            return Decision.deny("DAILY_LIMIT",
                    "已达日主动推送上限 " + properties.getDailyPushLimit(),
                    now);
        }
        return Decision.allow(now);
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
