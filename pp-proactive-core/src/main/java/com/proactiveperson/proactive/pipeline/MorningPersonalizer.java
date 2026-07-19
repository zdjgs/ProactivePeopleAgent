package com.proactiveperson.proactive.pipeline;

import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.memory.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Personalizer：从 Mem0 长期层取画像/偏好，与研究素材做匹配提示。
 */
@Component
public class MorningPersonalizer {

    private static final Logger log = LoggerFactory.getLogger(MorningPersonalizer.class);

    private final MemoryService memoryService;

    public MorningPersonalizer(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    public PersonalizedBrief personalize(ResearchBrief research) {
        List<String> profileHints;
        try {
            profileHints = memoryService.search(
                    research.user().userId(),
                    MemoryLayer.LONG_TERM,
                    "偏好 习惯 兴趣",
                    5);
        } catch (Exception ex) {
            log.warn("mem0 personalize search failed userId={} cause={}",
                    research.user().userId(), ex.getMessage());
            profileHints = List.of();
        }

        String toneHint = profileHints.isEmpty()
                ? "温暖幽默，像老朋友，不要客服腔"
                : "结合用户已知偏好，温暖幽默，像老朋友";

        return new PersonalizedBrief(research, profileHints, toneHint);
    }
}
