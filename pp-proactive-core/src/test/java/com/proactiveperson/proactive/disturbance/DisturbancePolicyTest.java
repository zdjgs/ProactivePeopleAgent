package com.proactiveperson.proactive.disturbance;

import com.proactiveperson.monitor.ActivityStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DisturbancePolicyTest {

    private final ZonedDateTime noon = ZonedDateTime.of(2026, 7, 19, 12, 0, 0, 0, ZoneId.of("Asia/Shanghai"));

    @Test
    void quietBlocksAll() {
        var decision = DisturbancePolicy.evaluate(
                DisturbancePreference.quiet(), PushPriority.HIGH, ActivityStatus.IDLE, noon);
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("QUIET_MODE");
    }

    @Test
    void importantOnlyAllowsHigh() {
        DisturbancePreference pref = new DisturbancePreference(
                DisturbanceMode.IMPORTANT_ONLY, null, null, null, Set.of("deadline"));
        assertThat(DisturbancePolicy.evaluate(pref, PushPriority.HIGH, ActivityStatus.IDLE, noon).allowed()).isTrue();
        assertThat(DisturbancePolicy.evaluate(pref, PushPriority.NORMAL, ActivityStatus.IDLE, noon).code())
                .isEqualTo("IMPORTANT_ONLY");
    }

    @Test
    void customHoursAllowsHighInsideQuietWindow() {
        DisturbancePreference pref = new DisturbancePreference(
                DisturbanceMode.CUSTOM_HOURS,
                LocalTime.of(10, 0),
                LocalTime.of(18, 0),
                null,
                Set.of());
        ZonedDateTime inWindow = noon; // 12:00
        assertThat(DisturbancePolicy.evaluate(pref, PushPriority.NORMAL, ActivityStatus.IDLE, inWindow).code())
                .isEqualTo("CUSTOM_QUIET_HOURS");
        assertThat(DisturbancePolicy.evaluate(pref, PushPriority.HIGH, ActivityStatus.IDLE, inWindow).allowed())
                .isTrue();
    }

    @Test
    void mutedUntilBlocks() {
        DisturbancePreference pref = DisturbancePreference.normal()
                .withMutedUntil(Instant.parse("2026-07-19T16:00:00Z"));
        assertThat(DisturbancePolicy.evaluate(pref, PushPriority.HIGH, ActivityStatus.IDLE, noon).code())
                .isEqualTo("MUTED_TODAY");
    }

    @Test
    void busyBlocksNormalButAllowsHigh() {
        assertThat(DisturbancePolicy.evaluate(
                DisturbancePreference.normal(), PushPriority.NORMAL, ActivityStatus.HIGH_INTENSITY, noon).code())
                .isEqualTo("BUSY");
        assertThat(DisturbancePolicy.evaluate(
                DisturbancePreference.normal(), PushPriority.HIGH, ActivityStatus.HIGH_INTENSITY, noon).allowed())
                .isTrue();
    }

    @Test
    void quietWindowSupportsOvernight() {
        assertThat(DisturbancePolicy.isInQuietWindow(LocalTime.of(23, 0), LocalTime.of(22, 0), LocalTime.of(8, 0)))
                .isTrue();
        assertThat(DisturbancePolicy.isInQuietWindow(LocalTime.of(7, 0), LocalTime.of(22, 0), LocalTime.of(8, 0)))
                .isTrue();
        assertThat(DisturbancePolicy.isInQuietWindow(LocalTime.of(12, 0), LocalTime.of(22, 0), LocalTime.of(8, 0)))
                .isFalse();
    }
}
