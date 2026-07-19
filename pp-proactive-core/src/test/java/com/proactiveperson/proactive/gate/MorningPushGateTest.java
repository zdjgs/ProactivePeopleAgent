package com.proactiveperson.proactive.gate;

import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.quota.DailyPushQuotaService;
import com.proactiveperson.proactive.user.ProactiveUser;
import com.proactiveperson.proactive.window.MorningPushWindowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MorningPushGateTest {

    private ProactiveProperties properties;
    private DailyPushQuotaService quotaService;
    private MorningPushWindowService windowService;

    @BeforeEach
    void setUp() {
        properties = new ProactiveProperties();
        properties.setDailyPushLimit(2);
        properties.setMorningWindowStartHour(8);
        properties.setMorningWindowEndHour(10);
        quotaService = new DailyPushQuotaService(properties);
        windowService = new MorningPushWindowService(properties);
    }

    @Test
    void allowsInsideWindow() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T01:30:00Z"), ZoneOffset.UTC); // 09:30 Shanghai
        MorningPushGate gate = MorningPushGate.createForTest(windowService, quotaService, properties, clock);
        ProactiveUser user = user(false);

        MorningPushGate.Decision decision = gate.evaluate(user);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.nowInUserZone().getZone()).isEqualTo(ZoneId.of("Asia/Shanghai"));
    }

    @Test
    void deniesOutsideWindow() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T03:00:00Z"), ZoneOffset.UTC); // 11:00 Shanghai
        MorningPushGate gate = MorningPushGate.createForTest(windowService, quotaService, properties, clock);

        assertThat(gate.evaluate(user(false)).code()).isEqualTo("OUTSIDE_MORNING_WINDOW");
    }

    @Test
    void deniesDoNotDisturb() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T01:00:00Z"), ZoneOffset.UTC);
        MorningPushGate gate = MorningPushGate.createForTest(windowService, quotaService, properties, clock);

        assertThat(gate.evaluate(user(true)).code()).isEqualTo("DO_NOT_DISTURB");
    }

    @Test
    void deniesWhenDailyLimitReached() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T01:00:00Z"), ZoneOffset.UTC);
        MorningPushGate gate = MorningPushGate.createForTest(windowService, quotaService, properties, clock);
        ProactiveUser user = user(false);
        var now = gate.evaluate(user).nowInUserZone();
        quotaService.recordPush(user, now);
        quotaService.recordPush(user, now);

        assertThat(gate.evaluate(user).code()).isEqualTo("DAILY_LIMIT");
    }

    private static ProactiveUser user(boolean dnd) {
        return new ProactiveUser("u1", "openid1", ZoneId.of("Asia/Shanghai"), true, "上海", dnd);
    }
}
