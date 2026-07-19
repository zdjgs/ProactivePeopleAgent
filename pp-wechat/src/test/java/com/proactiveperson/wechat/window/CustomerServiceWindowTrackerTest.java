package com.proactiveperson.wechat.window;

import com.proactiveperson.wechat.config.WeChatProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerServiceWindowTrackerTest {

    private WeChatProperties properties;
    private Instant fixedNow;

    @BeforeEach
    void setUp() {
        properties = new WeChatProperties();
        properties.setCustomerServiceWindowHours(48);
        fixedNow = Instant.parse("2026-07-16T10:00:00Z");
    }

    @Test
    void withinWindowAfterRecentInbound() {
        CustomerServiceWindowTracker tracker = CustomerServiceWindowTracker.createForTest(
                properties, Clock.fixed(fixedNow, ZoneOffset.UTC));
        tracker.recordInbound("o1", fixedNow.minus(Duration.ofHours(12)));

        assertThat(tracker.isWithinCustomerServiceWindow("o1")).isTrue();
        assertThat(tracker.isWithinCustomerServiceWindow("unknown")).isFalse();
    }

    @Test
    void outsideWindowAfter48Hours() {
        CustomerServiceWindowTracker tracker = CustomerServiceWindowTracker.createForTest(
                properties, Clock.fixed(fixedNow, ZoneOffset.UTC));
        tracker.recordInbound("o1", fixedNow.minus(Duration.ofHours(48)).minusSeconds(1));

        assertThat(tracker.isWithinCustomerServiceWindow("o1")).isFalse();
    }
}
