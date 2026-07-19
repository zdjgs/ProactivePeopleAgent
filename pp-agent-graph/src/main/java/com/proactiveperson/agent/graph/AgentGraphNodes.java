package com.proactiveperson.agent.graph;

import com.proactiveperson.agent.Assistant;
import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.memory.MemoryService;
import com.proactiveperson.mcp.McpToolClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 图节点动作：Researcher / Personalizer / Executor。
 */
public final class AgentGraphNodes {

    private static final Logger log = LoggerFactory.getLogger(AgentGraphNodes.class);

    private final McpToolClient mcpToolClient;
    private final MemoryService memoryService;
    private final ObjectProvider<Assistant> assistantProvider;

    public AgentGraphNodes(McpToolClient mcpToolClient,
                           MemoryService memoryService,
                           ObjectProvider<Assistant> assistantProvider) {
        this.mcpToolClient = mcpToolClient;
        this.memoryService = memoryService;
        this.assistantProvider = assistantProvider;
    }

    public Map<String, Object> research(AgentGraphState state) {
        String tool = pickResearchTool(state.query());
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("query", state.query());
        Object raw = mcpToolClient.callTool(tool, args);
        String notes = "tool=" + tool + "; result=" + stringify(raw);
        log.debug("researcher userId={} tool={} notesLength={}", state.userId(), tool, notes.length());
        return Map.of(AgentGraphState.RESEARCH_NOTES, notes);
    }

    public Map<String, Object> personalize(AgentGraphState state) {
        // 先按 query 检索，未命中则拉近期长期痕迹（画像/偏好常与字面 query 不重叠）
        List<String> hits = memoryService.search(state.userId(), MemoryLayer.LONG_TERM, state.query(), 5);
        if (hits == null || hits.isEmpty()) {
            hits = memoryService.search(state.userId(), MemoryLayer.LONG_TERM, "", 5);
        }
        String notes = hits == null || hits.isEmpty()
                ? "（暂无长期画像，使用默认陪伴语气）"
                : String.join(" | ", hits);
        log.debug("personalizer userId={} hits={}", state.userId(), hits == null ? 0 : hits.size());
        return Map.of(AgentGraphState.PERSONA_NOTES, notes);
    }

    public Map<String, Object> execute(AgentGraphState state) {
        String draft = buildDraft(state);
        String answer = polishIfPossible(draft);
        return Map.of(AgentGraphState.FINAL_ANSWER, answer);
    }

    private String polishIfPossible(String draft) {
        Assistant assistant = assistantProvider.getIfAvailable();
        if (assistant == null) {
            return draft;
        }
        try {
            String polished = assistant.complete(
                    "请把下面内容润色成简洁、有温度的中文回复，不要编造事实：\n" + draft);
            return polished == null || polished.isBlank() ? draft : polished.trim();
        } catch (RuntimeException ex) {
            log.warn("executor llm polish failed, fallback to template: {}", ex.getMessage());
            return draft;
        }
    }

    private static String buildDraft(AgentGraphState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("关于「").append(state.query()).append("」：\n");
        state.researchNotes().ifPresent(n -> sb.append("- 调研：").append(n).append('\n'));
        state.personaNotes().ifPresent(n -> sb.append("- 画像：").append(n).append('\n'));
        if (state.researchNotes().isEmpty() && state.personaNotes().isEmpty()) {
            sb.append("- 我先按你的节奏陪你想一想下一步。");
        } else {
            sb.append("如果你愿意，我们可以再往下拆一小步。");
        }
        return sb.toString().trim();
    }

    static String pickResearchTool(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
        if (q.contains("新闻") || q.contains("资讯") || q.contains("news")) {
            return "news";
        }
        return "weather";
    }

    private static String stringify(Object raw) {
        if (raw == null) {
            return "";
        }
        if (raw instanceof Map<?, ?> map) {
            Object summary = map.get("summary");
            if (summary != null) {
                return String.valueOf(summary);
            }
        }
        return String.valueOf(raw);
    }
}
