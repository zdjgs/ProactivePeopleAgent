package com.proactiveperson.proactive.pipeline;

import java.util.List;

/**
 * Personalizer 阶段产出：画像匹配后的个性化要点。
 */
public record PersonalizedBrief(
        ResearchBrief research,
        List<String> profileHints,
        String toneHint
) {
}
