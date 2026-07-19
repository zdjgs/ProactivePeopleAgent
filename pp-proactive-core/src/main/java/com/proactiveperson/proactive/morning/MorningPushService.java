package com.proactiveperson.proactive.morning;

import com.proactiveperson.proactive.config.DisturbanceProperties;
import com.proactiveperson.proactive.gate.MorningPushGate;
import com.proactiveperson.proactive.location.LocationContextResolver;
import com.proactiveperson.proactive.pipeline.MorningGenerator;
import com.proactiveperson.proactive.pipeline.MorningPersonalizer;
import com.proactiveperson.proactive.pipeline.MorningResearcher;
import com.proactiveperson.proactive.pipeline.PersonalizedBrief;
import com.proactiveperson.proactive.pipeline.ResearchBrief;
import com.proactiveperson.proactive.quota.DailyPushQuotaService;
import com.proactiveperson.proactive.user.ProactiveUser;
import com.proactiveperson.wechat.WeChatMessageGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 早间主动对话编排：门禁 → 配额预占 → 定位 → Research → Personalize → Generate → 微信推送。
 */
@Service
public class MorningPushService {

    private static final Logger log = LoggerFactory.getLogger(MorningPushService.class);

    private final MorningPushGate gate;
    private final LocationContextResolver locationResolver;
    private final MorningResearcher researcher;
    private final MorningPersonalizer personalizer;
    private final MorningGenerator generator;
    private final WeChatMessageGateway weChatMessageGateway;
    private final DailyPushQuotaService quotaService;
    private final DisturbanceProperties disturbanceProperties;

    public MorningPushService(MorningPushGate gate,
                              LocationContextResolver locationResolver,
                              MorningResearcher researcher,
                              MorningPersonalizer personalizer,
                              MorningGenerator generator,
                              WeChatMessageGateway weChatMessageGateway,
                              DailyPushQuotaService quotaService,
                              DisturbanceProperties disturbanceProperties) {
        this.gate = gate;
        this.locationResolver = locationResolver;
        this.researcher = researcher;
        this.personalizer = personalizer;
        this.generator = generator;
        this.weChatMessageGateway = weChatMessageGateway;
        this.quotaService = quotaService;
        this.disturbanceProperties = disturbanceProperties;
    }

    public PushOutcome pushForUser(ProactiveUser user) {
        MorningPushGate.Decision decision = gate.evaluate(user);
        if (!decision.allowed()) {
            log.info("morning push skipped userId={} code={} reason={}",
                    user.userId(), decision.code(), decision.reason());
            return PushOutcome.skipped(decision.code(), decision.reason());
        }

        DailyPushQuotaService.ReserveResult reserved =
                quotaService.tryReserveMorning(user, decision.nowInUserZone());
        if (reserved != DailyPushQuotaService.ReserveResult.RESERVED) {
            String code = reserved.name();
            String reason = reserved == DailyPushQuotaService.ReserveResult.ALREADY_MORNING_TODAY
                    ? "今日早间主动消息已推送，每日仅一次"
                    : "已达日主动推送上限（预占失败）";
            log.info("morning push reserve skipped userId={} code={}", user.userId(), code);
            return PushOutcome.skipped(code, reason);
        }

        try {
            return doPush(user, decision);
        } catch (RuntimeException ex) {
            quotaService.releaseMorning(user, decision.nowInUserZone());
            throw ex;
        }
    }

    private PushOutcome doPush(ProactiveUser user, MorningPushGate.Decision decision) {
        var location = locationResolver.resolve(user);
        if (!location.authorized()) {
            log.info("morning push location degraded userId={} note={}", user.userId(), location.note());
        }

        ResearchBrief research = researcher.research(user, location);
        PersonalizedBrief personalized = personalizer.personalize(research);
        String message = appendMuteHint(generator.generate(personalized));

        WeChatMessageGateway.SendResult send = weChatMessageGateway.sendTextAuto(user.openId(), message);
        if (!send.success()) {
            quotaService.releaseMorning(user, decision.nowInUserZone());
            log.warn("morning push send failed userId={} openId={} channel={} detail={}",
                    user.userId(), user.openId(), send.channel(), send.detail());
            return PushOutcome.failed(send.detail(), message);
        }

        log.info("morning push sent userId={} openId={} channel={} messageId={} sentAtLocal={} locationAuth={} cacheFallback={} contentLength={}",
                user.userId(),
                user.openId(),
                send.channel(),
                send.messageId(),
                decision.nowInUserZone(),
                location.authorized(),
                research.usedCacheFallback(),
                message.length());

        return PushOutcome.sent(send.messageId(), send.channel().name(), message);
    }

    private String appendMuteHint(String message) {
        if (!disturbanceProperties.isAppendMuteHint() || !StringUtils.hasText(disturbanceProperties.getMuteHint())) {
            return message;
        }
        String hint = disturbanceProperties.getMuteHint().trim();
        if (message != null && message.contains(hint)) {
            return message;
        }
        return message + "\n\n" + hint;
    }

    public record PushOutcome(boolean sent, boolean skipped, String code, String detail, String message) {

        public static PushOutcome skipped(String code, String detail) {
            return new PushOutcome(false, true, code, detail, null);
        }

        public static PushOutcome failed(String detail, String message) {
            return new PushOutcome(false, false, "SEND_FAILED", detail, message);
        }

        public static PushOutcome sent(String messageId, String channel, String message) {
            return new PushOutcome(true, false, "SENT", channel + ":" + messageId, message);
        }
    }
}
