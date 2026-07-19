package com.proactiveperson.proactive.quota;

import com.proactiveperson.common.state.InMemoryStateStore;
import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.user.ProactiveUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DailyPushQuotaServiceTest {

    private DailyPushQuotaService quotaService;
    private ProactiveUser user;
    private ZonedDateTime now;

    @BeforeEach
    void setUp() {
        ProactiveProperties properties = new ProactiveProperties();
        properties.setDailyPushLimit(2);
        quotaService = new DailyPushQuotaService(properties, new InMemoryStateStore());
        user = new ProactiveUser("u1", "o1", ZoneId.of("Asia/Shanghai"), true, "上海", false);
        now = ZonedDateTime.of(2026, 7, 19, 9, 0, 0, 0, ZoneId.of("Asia/Shanghai"));
    }

    @Test
    void tryReserveMorningOnlyOnce() {
        assertThat(quotaService.tryReserveMorning(user, now))
                .isEqualTo(DailyPushQuotaService.ReserveResult.RESERVED);
        assertThat(quotaService.tryReserveMorning(user, now))
                .isEqualTo(DailyPushQuotaService.ReserveResult.ALREADY_MORNING_TODAY);
        assertThat(quotaService.currentCount(user, now)).isEqualTo(1);
    }

    @Test
    void releaseAllowsRetry() {
        assertThat(quotaService.tryReserveMorning(user, now))
                .isEqualTo(DailyPushQuotaService.ReserveResult.RESERVED);
        quotaService.releaseMorning(user, now);
        assertThat(quotaService.tryReserveMorning(user, now))
                .isEqualTo(DailyPushQuotaService.ReserveResult.RESERVED);
    }

    @Test
    void dailyLimitBlocksEvenWithMorningSlotReleasedScenario() {
        ProactiveUser otherSlot = user;
        // 用完日上限（非早间槽）
        quotaService.recordPush(otherSlot, now);
        quotaService.recordPush(otherSlot, now);

        assertThat(quotaService.tryReserveMorning(user, now))
                .isEqualTo(DailyPushQuotaService.ReserveResult.DAILY_LIMIT);
        assertThat(quotaService.hasMorningSlot(user, now)).isTrue();
    }
}
