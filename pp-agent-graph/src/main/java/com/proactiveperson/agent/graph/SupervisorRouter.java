package com.proactiveperson.agent.graph;

import java.util.Locale;
import java.util.Map;

/**
 * 规则型 Supervisor：按缺口与关键词路由，不依赖 LLM。
 */
public final class SupervisorRouter {

    private SupervisorRouter() {
    }

    public static Map<String, Object> route(AgentGraphState state, int maxIterations) {
        int nextIteration = state.iteration() + 1;
        if (nextIteration > maxIterations) {
            return Map.of(
                    AgentGraphState.ITERATION, nextIteration,
                    AgentGraphState.NEXT, AgentGraphState.ROUTE_FINISH);
        }
        if (state.finalAnswer().filter(s -> !s.isBlank()).isPresent()) {
            return Map.of(
                    AgentGraphState.ITERATION, nextIteration,
                    AgentGraphState.NEXT, AgentGraphState.ROUTE_FINISH);
        }

        String query = state.query().toLowerCase(Locale.ROOT);
        boolean needsResearch = needsResearch(query);
        boolean needsPersona = needsPersona(query);

        if (needsResearch && state.researchNotes().filter(s -> !s.isBlank()).isEmpty()) {
            return Map.of(
                    AgentGraphState.ITERATION, nextIteration,
                    AgentGraphState.NEXT, AgentGraphState.ROUTE_RESEARCHER);
        }
        if (needsPersona && state.personaNotes().filter(s -> !s.isBlank()).isEmpty()) {
            return Map.of(
                    AgentGraphState.ITERATION, nextIteration,
                    AgentGraphState.NEXT, AgentGraphState.ROUTE_PERSONALIZER);
        }
        // 默认至少跑一次 personalizer，再 executor（保证可验收闭环）
        if (state.personaNotes().filter(s -> !s.isBlank()).isEmpty()) {
            return Map.of(
                    AgentGraphState.ITERATION, nextIteration,
                    AgentGraphState.NEXT, AgentGraphState.ROUTE_PERSONALIZER);
        }
        if (state.finalAnswer().filter(s -> !s.isBlank()).isEmpty()) {
            return Map.of(
                    AgentGraphState.ITERATION, nextIteration,
                    AgentGraphState.NEXT, AgentGraphState.ROUTE_EXECUTOR);
        }
        return Map.of(
                AgentGraphState.ITERATION, nextIteration,
                AgentGraphState.NEXT, AgentGraphState.ROUTE_FINISH);
    }

    static boolean needsResearch(String queryLower) {
        return containsAny(queryLower, "天气", "weather", "资讯", "新闻", "news", "外面", "研究", "查一下");
    }

    static boolean needsPersona(String queryLower) {
        return containsAny(queryLower, "偏好", "习惯", "画像", "喜欢", "风格", "个性", "我的");
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
