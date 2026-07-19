package com.proactiveperson.agent.graph;

import com.proactiveperson.agent.Assistant;
import com.proactiveperson.agent.config.AgentGraphRuntimeProperties;
import com.proactiveperson.memory.MemoryService;
import com.proactiveperson.mcp.McpToolClient;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.utils.EdgeMappings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 编译 Supervisor 最小图：START→supervisor⇄{researcher,personalizer,executor}→END。
 */
@Component
public class AgentGraphFactory {

    private final AgentGraphRuntimeProperties properties;
    private final AgentGraphNodes nodes;

    public AgentGraphFactory(AgentGraphRuntimeProperties properties,
                             McpToolClient mcpToolClient,
                             MemoryService memoryService,
                             ObjectProvider<Assistant> assistantProvider) {
        this.properties = properties;
        this.nodes = new AgentGraphNodes(mcpToolClient, memoryService, assistantProvider);
    }

    public CompiledGraph<AgentGraphState> compile() throws GraphStateException {
        int maxIterations = Math.max(1, properties.getMaxIterations());
        StateGraph<AgentGraphState> workflow = new StateGraph<>(AgentGraphState.SCHEMA, AgentGraphState::new)
                .addNode("supervisor", node_async(state -> SupervisorRouter.route(state, maxIterations)))
                .addNode("researcher", node_async(nodes::research))
                .addNode("personalizer", node_async(nodes::personalize))
                .addNode("executor", node_async(nodes::execute))
                .addEdge(START, "supervisor")
                .addConditionalEdges("supervisor",
                        edge_async(state -> state.next().orElse(AgentGraphState.ROUTE_FINISH)),
                        EdgeMappings.builder()
                                .to(AgentGraphState.ROUTE_RESEARCHER)
                                .to(AgentGraphState.ROUTE_PERSONALIZER)
                                .to(AgentGraphState.ROUTE_EXECUTOR)
                                .toEND(AgentGraphState.ROUTE_FINISH)
                                .build())
                .addEdge("researcher", "supervisor")
                .addEdge("personalizer", "supervisor")
                .addEdge("executor", "supervisor");

        CompiledGraph<AgentGraphState> compiled = workflow.compile();
        // 图级硬上限：节点跳转次数，避免条件边异常时死循环
        compiled.setMaxIterations(Math.max(8, maxIterations * 4));
        return compiled;
    }
}
