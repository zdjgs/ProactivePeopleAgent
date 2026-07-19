package com.proactiveperson.proactive.gate;

import com.proactiveperson.common.state.InMemoryStateStore;
import com.proactiveperson.memory.stub.InMemoryMemoryService;
import com.proactiveperson.monitor.ActivityMonitor;
import com.proactiveperson.monitor.ActivityStatus;
import com.proactiveperson.proactive.config.DisturbanceProperties;
import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.disturbance.DisturbanceMode;
import com.proactiveperson.proactive.disturbance.DisturbancePreferenceService;
import com.proactiveperson.proactive.disturbance.cache.InMemoryTtlPreferenceCache;
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
    private DisturbancePreferenceService preferenceService;
    private ActivityMonitor idleMonitor;

    @BeforeEach
    void setUp() {
        properties = new ProactiveProperties();
        properties.setDailyPushLimit(2);
        properties.setMorningWindowStartHour(8);
        properties.setMorningWindowEndHour(10);
        quotaService = new DailyPushQuotaService(properties, new InMemoryStateStore());
        windowService = new MorningPushWindowService(properties);
        preferenceService = new DisturbancePreferenceService(
                new InMemoryMemoryService(), new InMemoryTtlPreferenceCache(), new DisturbanceProperties());
        idleMonitor = userId -> ActivityStatus.IDLE;
    }

    @Test
    void allowsInsideWindow() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T01:30:00Z"), ZoneOffset.UTC); // 09:30 Shanghai
        MorningPushGate gate = gate(clock);
        ProactiveUser user = user(false);

        MorningPushGate.Decision decision = gate.evaluate(user);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.nowInUserZone().getZone()).isEqualTo(ZoneId.of("Asia/Shanghai"));
    }

    @Test
    void deniesOutsideWindow() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T03:00:00Z"), ZoneOffset.UTC); // 11:00 Shanghai
        MorningPushGate gate = gate(clock);

        assertThat(gate.evaluate(user(false)).code()).isEqualTo("OUTSIDE_MORNING_WINDOW");
    }

    @Test
    void deniesConfigDoNotDisturbAsQuiet() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T01:00:00Z"), ZoneOffset.UTC);
        MorningPushGate gate = gate(clock);

        assertThat(gate.evaluate(user(true)).code()).isEqualTo("QUIET_MODE");
    }

    @Test
    void deniesImportantOnlyForNormalMorningPush() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T01:00:00Z"), ZoneOffset.UTC);
        preferenceService.setMode("u1", DisturbanceMode.IMPORTANT_ONLY);
        MorningPushGate gate = gate(clock);

        assertThat(gate.evaluate(user(false)).code()).isEqualTo("IMPORTANT_ONLY");
    }

    @Test
    void deniesWhenDailyLimitReached() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T01:00:00Z"), ZoneOffset.UTC);
        MorningPushGate gate = gate(clock);
        ProactiveUser user = user(false);
        var now = java.time.ZonedDateTime.now(clock).withZoneSameInstant(user.timezone());
        quotaService.recordPush(user, now);
        quotaService.recordPush(user, now);

        assertThat(gate.evaluate(user).code()).isEqualTo("DAILY_LIMIT");
    }

    @Test
    void deniesWhenMorningAlreadyReserved() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T01:00:00Z"), ZoneOffset.UTC);
        MorningPushGate gate = gate(clock);
        ProactiveUser user = user(false);
        var now = java.time.ZonedDateTime.now(clock).withZoneSameInstant(user.timezone());
        assertThat(quotaService.tryReserveMorning(user, now))
                .isEqualTo(DailyPushQuotaService.ReserveResult.RESERVED);

        assertThat(gate.evaluate(user).code()).isEqualTo("ALREADY_MORNING_TODAY");
    }

    private MorningPushGate gate(Clock clock) {
        return MorningPushGate.createForTest(
                windowService, quotaService, properties, preferenceService, idleMonitor, clock);
    }

    private static ProactiveUser user(boolean dnd) {
        return new ProactiveUser("u1", "openid1", ZoneId.of("Asia/Shanghai"), true, "上海", dnd);
    }
}
