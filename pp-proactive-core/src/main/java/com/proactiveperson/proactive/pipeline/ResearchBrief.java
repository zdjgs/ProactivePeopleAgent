package com.proactiveperson.proactive.pipeline;

import com.proactiveperson.proactive.location.LocationContextResolver.LocationContext;
import com.proactiveperson.proactive.user.ProactiveUser;

import java.util.List;

/**
 * Researcher 阶段产出：从 MCP 拉取并过滤的实时素材。
 */
public record ResearchBrief(
        ProactiveUser user,
        LocationContext location,
        List<String> facts,
        boolean usedCacheFallback
) {
}
