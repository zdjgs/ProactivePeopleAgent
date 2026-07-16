package com.proactiveperson.proactive.schedule;

import com.proactiveperson.proactive.config.ProactiveProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 早间主动推送调度扩展点。
 * 真实逻辑（时区窗口 / 规则引擎 / MCP / Multi-Agent）在后续任务实现。
 */
@Component
@ConditionalOnProperty(name = "pp.proactive.morning-push-enabled", havingValue = "true")
public class MorningPushScheduler {

    private static final Logger log = LoggerFactory.getLogger(MorningPushScheduler.class);

    private final ProactiveProperties properties;

    public MorningPushScheduler(ProactiveProperties properties) {
        this.properties = properties;
    }

    /** 每分钟扫描一次候选用户；窗口校验在业务服务中完成 */
    @Scheduled(cron = "${pp.proactive.morning-scan-cron:0 */1 8-10 * * *}")
    public void scanMorningWindow() {
        log.debug("morning push scan tick, dailyLimit={}", properties.getDailyPushLimit());
    }
}
