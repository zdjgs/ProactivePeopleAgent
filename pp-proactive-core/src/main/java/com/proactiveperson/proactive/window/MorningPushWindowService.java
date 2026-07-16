package com.proactiveperson.proactive.window;

import com.proactiveperson.proactive.config.ProactiveProperties;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 早间推送时间窗口校验（用户本地时区 08:00–10:00，左闭右开）。
 */
@Service
public class MorningPushWindowService {

    private final ProactiveProperties properties;

    public MorningPushWindowService(ProactiveProperties properties) {
        this.properties = properties;
    }

    public boolean isInMorningWindow(ZoneId zoneId, ZonedDateTime instant) {
        ZonedDateTime local = instant.withZoneSameInstant(zoneId);
        LocalTime time = local.toLocalTime();
        LocalTime start = LocalTime.of(properties.getMorningWindowStartHour(), 0);
        LocalTime end = LocalTime.of(properties.getMorningWindowEndHour(), 0);
        return !time.isBefore(start) && time.isBefore(end);
    }
}
