package com.proactiveperson.proactive.pipeline;

import com.proactiveperson.agent.Assistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Generator：组装早间主动消息。
 * LLM 可用时走 {@link Assistant} 润色；否则用确定性模板（保证无 Key 也能跑通链路）。
 */
@Component
public class MorningGenerator {

    private static final Logger log = LoggerFactory.getLogger(MorningGenerator.class);

    private final ObjectProvider<Assistant> assistant;

    public MorningGenerator(ObjectProvider<Assistant> assistant) {
        this.assistant = assistant;
    }

    public String generate(PersonalizedBrief brief) {
        Assistant ai = assistant.getIfAvailable();
        if (ai != null) {
            try {
                // 无 ChatMemory，避免污染多轮会话
                return ai.complete(buildLlmPrompt(brief)).trim();
            } catch (Exception ex) {
                log.warn("morning llm generate failed, fallback template: {}", ex.getMessage());
            }
        }
        return buildTemplateMessage(brief);
    }

    static String buildTemplateMessage(PersonalizedBrief brief) {
        StringBuilder sb = new StringBuilder();
        sb.append("早呀～");
        if (brief.research().location().authorized()) {
            sb.append(brief.research().location().cityOrLabel()).append("的早晨到了。");
        } else {
            sb.append("新的一天开始了。");
        }

        if (!brief.research().facts().isEmpty()) {
            sb.append(brief.research().facts().getFirst());
            if (!brief.research().facts().getFirst().endsWith("。")
                    && !brief.research().facts().getFirst().endsWith("！")
                    && !brief.research().facts().getFirst().endsWith("？")) {
                sb.append("。");
            }
        }

        if (!brief.profileHints().isEmpty()) {
            sb.append("想起你提到过：").append(brief.profileHints().getFirst());
            if (!brief.profileHints().getFirst().endsWith("。")) {
                sb.append("。");
            }
        }

        sb.append("今天想先从哪件小事开始？");
        return sb.toString();
    }

    private static String buildLlmPrompt(PersonalizedBrief brief) {
        return """
                请生成一条「主动的人」早间主动关怀微信消息（中文，2-4 句，口语温暖）。
                语气要求：%s
                位置：%s（%s）
                实时素材：%s
                用户画像提示：%s
                不要客服腔，不要列表，结尾用一个轻松的小问题邀请回复。
                """.formatted(
                brief.toneHint(),
                brief.research().location().cityOrLabel(),
                brief.research().location().note(),
                brief.research().facts(),
                brief.profileHints().isEmpty() ? "（暂无长期画像）" : brief.profileHints());
    }
}
