package com.proactiveperson.proactive.task;

import com.proactiveperson.agent.Assistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

/**
 * 苏格拉底式提醒文案：提问引导，禁止空洞催促。
 */
@Component
public class SocraticReminderComposer {

    private static final Logger log = LoggerFactory.getLogger(SocraticReminderComposer.class);

    private static final Set<String> FORBIDDEN = Set.of(
            "赶紧", "必须完成", "立刻去做", "还不做", "赶快完成", "马上完成");

    private final ObjectProvider<Assistant> assistantProvider;

    public SocraticReminderComposer(ObjectProvider<Assistant> assistantProvider) {
        this.assistantProvider = assistantProvider;
    }

    public String compose(Task task, ZoneId zoneId) {
        String template = buildTemplate(task, zoneId);
        Assistant assistant = assistantProvider.getIfAvailable();
        if (assistant == null) {
            return template;
        }
        try {
            String polished = assistant.complete("""
                    把下面提醒改成苏格拉底式提问（中文，1-3 句），禁止催促/说教，保留任务主题：
                    %s
                    """.formatted(template)).trim();
            if (polished.isBlank() || containsForbidden(polished) || !polished.contains("？") && !polished.contains("?")) {
                return template;
            }
            return polished;
        } catch (RuntimeException ex) {
            log.warn("socratic polish failed, use template: {}", ex.getMessage());
            return template;
        }
    }

    static String buildTemplate(Task task, ZoneId zoneId) {
        String dueHint = "";
        if (task.dueAt() != null) {
            String when = DateTimeFormatter.ofPattern("M月d日 HH:mm")
                    .withZone(zoneId == null ? ZoneId.of("Asia/Shanghai") : zoneId)
                    .format(task.dueAt());
            dueHint = "（大约在 " + when + " 前后）";
        }
        return "关于「" + task.title() + "」" + dueHint
                + "，你现在最卡的是哪一步？如果只推进 10 分钟，你会先动哪一件小事？";
    }

    static boolean containsForbidden(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String word : FORBIDDEN) {
            if (lower.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** 测试辅助：当前时刻是否在截止窗口内（由 resolver 使用）。 */
    public static boolean dueWithin(Instant dueAt, Instant now, int hours) {
        if (dueAt == null || now == null) {
            return false;
        }
        long delta = dueAt.getEpochSecond() - now.getEpochSecond();
        return delta >= 0 && delta <= hours * 3600L;
    }
}
