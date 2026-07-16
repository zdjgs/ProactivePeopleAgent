package com.proactiveperson.proactive.schedule;

import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.window.MorningPushWindowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 早间主动推送调度扩展点。
 * 当前仅做时区窗口门禁与用户扫描占位；完整推送链路见 T-005。
 */
@Component
@ConditionalOnProperty(name = "pp.proactive.morning-push-enabled", havingValue = "true")
public class MorningPushScheduler {

    private static final Logger log = LoggerFactory.getLogger(MorningPushScheduler.class);

    private final ProactiveProperties properties;
    private final MorningPushWindowService windowService;

    public MorningPushScheduler(ProactiveProperties properties,
                                MorningPushWindowService windowService) {
        this.properties = properties;
        this.windowService = windowService;
    }

    @Scheduled(cron = "${pp.proactive.morning-scan-cron:0 */1 8-10 * * *}")
    public void scanMorningWindow() {
        ZoneId zoneId = ZoneId.of(properties.getDefaultTimezone());
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        if (!windowService.isInMorningWindow(zoneId, now)) {
            log.trace("skip morning scan: outside window zone={} localTime={}",
                    zoneId, now.toLocalTime());
            return;
        }

        log.debug("morning window open zone={} localTime={} dailyLimit={} — user scan placeholder",
                zoneId, now.toLocalTime(), properties.getDailyPushLimit());
        // T-005: 遍历候选用户 → 规则引擎 → MCP → Multi-Agent → 微信推送
    }
}
