package com.proactiveperson.proactive.pipeline;

import com.proactiveperson.mcp.McpToolClient;
import com.proactiveperson.proactive.location.LocationContextResolver.LocationContext;
import com.proactiveperson.proactive.user.ProactiveUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Researcher：调用 MCP（天气/新闻等）并过滤为空的结果；超时/失败则缓存降级。
 */
@Component
public class MorningResearcher {

    private static final Logger log = LoggerFactory.getLogger(MorningResearcher.class);

    private final McpToolClient mcpToolClient;
    private final Map<String, List<String>> lastGoodFacts = new ConcurrentHashMap<>();

    public MorningResearcher(McpToolClient mcpToolClient) {
        this.mcpToolClient = mcpToolClient;
    }

    public ResearchBrief research(ProactiveUser user, LocationContext location) {
        List<String> facts = new ArrayList<>();
        boolean usedCache = false;

        facts.addAll(safeCall("weather", Map.of(
                "city", location.cityOrLabel(),
                "userId", user.userId())));
        facts.addAll(safeCall("news", Map.of(
                "topic", "morning",
                "userId", user.userId())));

        facts = facts.stream().filter(f -> f != null && !f.isBlank()).distinct().limit(6).toList();

        if (facts.isEmpty()) {
            List<String> cached = lastGoodFacts.get(user.userId());
            if (cached != null && !cached.isEmpty()) {
                facts = cached;
                usedCache = true;
                log.warn("morning research fallback to cache userId={}", user.userId());
            } else {
                facts = List.of("今天也适合慢慢开始，先给自己一点缓冲时间。");
                usedCache = true;
            }
        } else {
            lastGoodFacts.put(user.userId(), facts);
        }

        return new ResearchBrief(user, location, facts, usedCache);
    }

    private List<String> safeCall(String tool, Map<String, Object> args) {
        try {
            Object result = mcpToolClient.callTool(tool, args);
            return normalize(result);
        } catch (Exception ex) {
            log.warn("mcp tool {} failed: {}", tool, ex.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> normalize(Object result) {
        if (result == null) {
            return List.of();
        }
        if (result instanceof String s) {
            return s.isBlank() ? List.of() : List.of(s);
        }
        if (result instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(s -> !s.isBlank()).toList();
        }
        if (result instanceof Map<?, ?> map) {
            Object summary = map.get("summary");
            if (summary != null && !String.valueOf(summary).isBlank()) {
                return List.of(String.valueOf(summary));
            }
            Object status = map.get("status");
            if ("stub-not-configured".equals(String.valueOf(status))) {
                return List.of();
            }
        }
        return List.of(String.valueOf(result));
    }
}
