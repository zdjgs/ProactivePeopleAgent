package com.proactiveperson.agent.graph;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SupervisorRouterTest {

    @Test
    void routesWeatherToResearcher() {
        AgentGraphState state = new AgentGraphState(Map.of(
                AgentGraphState.QUERY, "今天天气如何",
                AgentGraphState.ITERATION, 0));

        Map<String, Object> update = SupervisorRouter.route(state, 5);

        assertThat(update.get(AgentGraphState.NEXT)).isEqualTo(AgentGraphState.ROUTE_RESEARCHER);
        assertThat(update.get(AgentGraphState.ITERATION)).isEqualTo(1);
    }

    @Test
    void finishesWhenMaxIterationsExceeded() {
        AgentGraphState state = new AgentGraphState(Map.of(
                AgentGraphState.QUERY, "今天天气如何",
                AgentGraphState.ITERATION, 5));

        Map<String, Object> update = SupervisorRouter.route(state, 5);

        assertThat(update.get(AgentGraphState.NEXT)).isEqualTo(AgentGraphState.ROUTE_FINISH);
    }

    @Test
    void routesToExecutorWhenNotesReady() {
        AgentGraphState state = new AgentGraphState(Map.of(
                AgentGraphState.QUERY, "随便聊聊",
                AgentGraphState.ITERATION, 1,
                AgentGraphState.PERSONA_NOTES, "偏好温和"));

        Map<String, Object> update = SupervisorRouter.route(state, 5);

        assertThat(update.get(AgentGraphState.NEXT)).isEqualTo(AgentGraphState.ROUTE_EXECUTOR);
    }
}
