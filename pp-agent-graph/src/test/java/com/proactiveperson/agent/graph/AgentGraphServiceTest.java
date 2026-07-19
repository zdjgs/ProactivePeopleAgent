package com.proactiveperson.agent.graph;

import com.proactiveperson.agent.config.AgentGraphRuntimeProperties;
import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.memory.MemoryService;
import com.proactiveperson.memory.stub.InMemoryMemoryService;
import com.proactiveperson.mcp.McpToolClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentGraphServiceTest {

    @Mock
    private McpToolClient mcpToolClient;
    @Mock
    private ObjectProvider<com.proactiveperson.agent.Assistant> assistantProvider;

    private MemoryService memoryService;
    private AgentGraphRuntimeProperties properties;

    @BeforeEach
    void setUp() {
        memoryService = new InMemoryMemoryService();
        properties = new AgentGraphRuntimeProperties();
        properties.setMaxIterations(5);
        lenient().when(assistantProvider.getIfAvailable()).thenReturn(null);
    }

    @Test
    void runsSupervisorGraphForWeatherQueryAndPersistsMidTerm() {
        when(mcpToolClient.callTool(eq("weather"), any())).thenReturn(Map.of("summary", "晴朗宜人"));

        AgentGraphFactory factory = new AgentGraphFactory(properties, mcpToolClient, memoryService, assistantProvider);
        AgentGraphService service = new AgentGraphService(factory, memoryService);

        AgentGraphService.AgentGraphResult result = service.run("u1", "今天天气怎么样");

        assertThat(result.answer()).isNotBlank();
        assertThat(result.researchNotes()).contains("晴朗宜人");
        assertThat(result.iterations()).isGreaterThan(0);
        verify(mcpToolClient, atLeastOnce()).callTool(eq("weather"), any());

        List<String> mid = memoryService.search("u1", MemoryLayer.MID_TERM, "agent_graph", 5);
        assertThat(mid).isNotEmpty();
        assertThat(mid.getFirst()).contains("agent_graph");
    }

    @Test
    void respectsMaxIterationsAndStillReturns() {
        properties.setMaxIterations(1);
        lenient().when(mcpToolClient.callTool(anyString(), any())).thenReturn(Map.of("summary", "x"));

        AgentGraphFactory factory = new AgentGraphFactory(properties, mcpToolClient, memoryService, assistantProvider);
        AgentGraphService service = new AgentGraphService(factory, memoryService);

        AgentGraphService.AgentGraphResult result = service.run("u1", "查一下天气和新闻");

        // maxIterations=1：researcher 后再次 supervisor 即 FINISH（可能无终稿，走 fallback）
        assertThat(result.iterations()).isLessThanOrEqualTo(2);
        assertThat(result.answer()).isNotBlank();
        assertThat(memoryService.search("u1", MemoryLayer.MID_TERM, "agent_graph", 5)).isNotEmpty();
    }

    @Test
    void personalizerUsesLongTermMemory() {
        memoryService.add("u1", MemoryLayer.LONG_TERM, "喜欢早晨咖啡");
        AgentGraphFactory factory = new AgentGraphFactory(properties, mcpToolClient, memoryService, assistantProvider);
        AgentGraphService service = new AgentGraphService(factory, memoryService);

        AgentGraphService.AgentGraphResult result = service.run("u1", "按我的偏好聊聊");

        assertThat(result.personaNotes()).contains("喜欢早晨咖啡");
        assertThat(result.answer()).contains("画像");
    }
}
