package com.proactiveperson.proactive.task;

import com.proactiveperson.common.state.InMemoryStateStore;
import com.proactiveperson.memory.stub.InMemoryMemoryService;
import com.proactiveperson.monitor.ActivityStatus;
import com.proactiveperson.proactive.config.DisturbanceProperties;
import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.config.TaskProperties;
import com.proactiveperson.proactive.disturbance.DisturbanceMode;
import com.proactiveperson.proactive.disturbance.DisturbancePreference;
import com.proactiveperson.proactive.disturbance.DisturbancePreferenceService;
import com.proactiveperson.proactive.disturbance.PushPriority;
import com.proactiveperson.proactive.disturbance.cache.InMemoryTtlPreferenceCache;
import com.proactiveperson.proactive.quota.DailyPushQuotaService;
import com.proactiveperson.proactive.user.ProactiveUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TaskFollowUpGateTest {

    private DisturbancePreferenceService preferenceService;
    private DailyPushQuotaService quotaService;
    private TaskFollowUpGate gate;
    private ProactiveUser user;

    @BeforeEach
    void setUp() {
        preferenceService = new DisturbancePreferenceService(
                new InMemoryMemoryService(), new InMemoryTtlPreferenceCache(), new DisturbanceProperties());
        ProactiveProperties properties = new ProactiveProperties();
        properties.setDailyPushLimit(2);
        quotaService = new DailyPushQuotaService(properties, new InMemoryStateStore());
        Clock clock = Clock.fixed(Instant.parse("2026-07-20T01:00:00Z"), ZoneOffset.UTC);
        gate = TaskFollowUpGate.createForTest(preferenceService, userId -> ActivityStatus.IDLE, quotaService, clock);
        user = new ProactiveUser("u1", "oid", ZoneId.of("Asia/Shanghai"), true, "上海", false);
    }

    @Test
    void allowsNormalWhenModeNormal() {
        assertThat(gate.evaluate(user, PushPriority.NORMAL).allowed()).isTrue();
    }

    @Test
    void quietBlocksAll() {
        preferenceService.save("u1", DisturbancePreference.quiet());
        assertThat(gate.evaluate(user, PushPriority.HIGH).code()).isEqualTo("QUIET_MODE");
    }

    @Test
    void importantOnlyAllowsHigh() {
        preferenceService.save("u1", new DisturbancePreference(
                DisturbanceMode.IMPORTANT_ONLY, null, null, null, Set.of("工作")));
        assertThat(gate.evaluate(user, PushPriority.NORMAL).code()).isEqualTo("IMPORTANT_ONLY");
        assertThat(gate.evaluate(user, PushPriority.HIGH).allowed()).isTrue();
    }

    @Test
    void busyBlocksNormal() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-20T01:00:00Z"), ZoneOffset.UTC);
        gate = TaskFollowUpGate.createForTest(
                preferenceService, userId -> ActivityStatus.HIGH_INTENSITY, quotaService, clock);
        assertThat(gate.evaluate(user, PushPriority.NORMAL).code()).isEqualTo("BUSY");
        assertThat(gate.evaluate(user, PushPriority.HIGH).allowed()).isTrue();
    }
}
