package com.proactiveperson.proactive.window;

import com.proactiveperson.proactive.config.ProactiveProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MorningPushWindowServiceTest {

    private MorningPushWindowService windowService;
    private ZoneId shanghai;

    @BeforeEach
    void setUp() {
        ProactiveProperties properties = new ProactiveProperties();
        properties.setMorningWindowStartHour(8);
        properties.setMorningWindowEndHour(10);
        windowService = new MorningPushWindowService(properties);
        shanghai = ZoneId.of("Asia/Shanghai");
    }

    @Test
    void insideWindowAtEightThirty() {
        ZonedDateTime instant = ZonedDateTime.of(2026, 7, 16, 8, 30, 0, 0, shanghai);
        assertThat(windowService.isInMorningWindow(shanghai, instant)).isTrue();
    }

    @Test
    void outsideWindowAtTen() {
        ZonedDateTime instant = ZonedDateTime.of(2026, 7, 16, 10, 0, 0, 0, shanghai);
        assertThat(windowService.isInMorningWindow(shanghai, instant)).isFalse();
    }

    @Test
    void outsideWindowAtSevenFiftyNine() {
        ZonedDateTime instant = ZonedDateTime.of(2026, 7, 16, 7, 59, 0, 0, shanghai);
        assertThat(windowService.isInMorningWindow(shanghai, instant)).isFalse();
    }
}
