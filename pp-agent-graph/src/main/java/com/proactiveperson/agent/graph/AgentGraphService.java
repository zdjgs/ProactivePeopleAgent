package com.proactiveperson.agent.graph;

import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.common.exception.MemoryInvocationException;
import com.proactiveperson.memory.MemoryService;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Supervisor 图编排入口（不替换 DefaultChatService）。
 */
@Service
public class AgentGraphService {

    private static final Logger log = LoggerFactory.getLogger(AgentGraphService.class);

    private final AgentGraphFactory graphFactory;
    private final MemoryService memoryService;

    public AgentGraphService(AgentGraphFactory graphFactory, MemoryService memoryService) {
        this.graphFactory = graphFactory;
        this.memoryService = memoryService;
    }

    public AgentGraphResult run(String userId, String query) {
        String resolvedUserId = StringUtils.hasText(userId) ? userId : "anonymous";
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("query 不能为空");
        }
        try {
            CompiledGraph<AgentGraphState> graph = graphFactory.compile();
            Map<String, Object> input = new LinkedHashMap<>();
            input.put(AgentGraphState.USER_ID, resolvedUserId);
            input.put(AgentGraphState.QUERY, query.trim());
            input.put(AgentGraphState.ITERATION, 0);

            Optional<AgentGraphState> finalState = graph.invoke(input);
            AgentGraphState state = finalState.orElseThrow(
                    () -> new IllegalStateException("Supervisor 图未返回终态"));

            String answer = state.finalAnswer().orElseGet(() -> fallbackAnswer(state));
            persistMidTerm(resolvedUserId, query, answer, state.iteration());
            return new AgentGraphResult(
                    resolvedUserId,
                    query.trim(),
                    answer,
                    state.iteration(),
                    state.researchNotes().orElse(null),
                    state.personaNotes().orElse(null));
        } catch (GraphStateException ex) {
            throw new IllegalStateException("Supervisor 图编译/执行失败: " + ex.getMessage(), ex);
        }
    }

    private void persistMidTerm(String userId, String query, String answer, int iterations) {
        try {
            String content = "agent_graph: q=" + query + "; iterations=" + iterations + "; a=" + answer;
            memoryService.add(userId, MemoryLayer.MID_TERM, content);
        } catch (MemoryInvocationException ex) {
            log.warn("agent graph mid-term persist failed userId={} cause={}", userId, ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("agent graph mid-term persist failed userId={} cause={}", userId, ex.getMessage());
        }
    }

    private static String fallbackAnswer(AgentGraphState state) {
        StringBuilder sb = new StringBuilder("（达到迭代上限或未生成终稿）");
        state.researchNotes().ifPresent(n -> sb.append(" 调研：").append(n));
        state.personaNotes().ifPresent(n -> sb.append(" 画像：").append(n));
        return sb.toString();
    }

    public record AgentGraphResult(
            String userId,
            String query,
            String answer,
            int iterations,
            String researchNotes,
            String personaNotes
    ) {
    }
}
