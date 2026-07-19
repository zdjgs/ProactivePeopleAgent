package com.proactiveperson.proactive.schedule;

import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.morning.MorningPushService;
import com.proactiveperson.proactive.user.ProactiveUser;
import com.proactiveperson.proactive.user.ProactiveUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 早间主动推送扫描：遍历候选用户，交由 {@link MorningPushService} 按用户时区做窗口与门禁。
 * <p>
 * cron 使用宽扫描（默认可每分钟）；真正是否推送由每位用户的本地 08:00–10:00 判定。
 */
@Component
@ConditionalOnProperty(name = "pp.proactive.morning-push-enabled", havingValue = "true")
public class MorningPushScheduler {

    private static final Logger log = LoggerFactory.getLogger(MorningPushScheduler.class);

    private final ProactiveProperties properties;
    private final ProactiveUserRepository userRepository;
    private final MorningPushService morningPushService;

    public MorningPushScheduler(ProactiveProperties properties,
                                ProactiveUserRepository userRepository,
                                MorningPushService morningPushService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.morningPushService = morningPushService;
    }

    @Scheduled(cron = "${pp.proactive.morning-scan-cron:0 */1 * * * *}")
    public void scanMorningWindow() {
        List<ProactiveUser> candidates = userRepository.findAllCandidates();
        if (candidates.isEmpty()) {
            log.trace("morning scan: no candidates configured (pp.proactive.users)");
            return;
        }

        int sent = 0;
        int skipped = 0;
        int failed = 0;
        for (ProactiveUser user : candidates) {
            try {
                MorningPushService.PushOutcome outcome = morningPushService.pushForUser(user);
                if (outcome.sent()) {
                    sent++;
                } else if (outcome.skipped()) {
                    skipped++;
                } else {
                    failed++;
                }
            } catch (Exception ex) {
                failed++;
                log.warn("morning push unexpected error userId={} cause={}", user.userId(), ex.getMessage());
            }
        }

        log.debug("morning scan done candidates={} sent={} skipped={} failed={} dailyLimit={}",
                candidates.size(), sent, skipped, failed, properties.getDailyPushLimit());
    }
}
