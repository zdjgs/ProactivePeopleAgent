package com.proactiveperson.agent.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Supervisor 图状态：末写覆盖语义。
 */
public class AgentGraphState extends AgentState {

    public static final String USER_ID = "userId";
    public static final String QUERY = "query";
    public static final String NEXT = "next";
    public static final String ITERATION = "iteration";
    public static final String RESEARCH_NOTES = "researchNotes";
    public static final String PERSONA_NOTES = "personaNotes";
    public static final String FINAL_ANSWER = "finalAnswer";

    public static final String ROUTE_RESEARCHER = "researcher";
    public static final String ROUTE_PERSONALIZER = "personalizer";
    public static final String ROUTE_EXECUTOR = "executor";
    public static final String ROUTE_FINISH = "FINISH";

    public static final Map<String, Channel<?>> SCHEMA;

    static {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put(USER_ID, Channels.base((a, b) -> b));
        channels.put(QUERY, Channels.base((a, b) -> b));
        channels.put(NEXT, Channels.base((a, b) -> b));
        channels.put(ITERATION, Channels.base((a, b) -> b));
        channels.put(RESEARCH_NOTES, Channels.base((a, b) -> b));
        channels.put(PERSONA_NOTES, Channels.base((a, b) -> b));
        channels.put(FINAL_ANSWER, Channels.base((a, b) -> b));
        SCHEMA = Map.copyOf(channels);
    }

    public AgentGraphState(Map<String, Object> initData) {
        super(initData);
    }

    public String userId() {
        return value(USER_ID, "");
    }

    public String query() {
        return value(QUERY, "");
    }

    public Optional<String> next() {
        return value(NEXT);
    }

    public int iteration() {
        Object raw = value(ITERATION).orElse(0);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    public Optional<String> researchNotes() {
        return value(RESEARCH_NOTES);
    }

    public Optional<String> personaNotes() {
        return value(PERSONA_NOTES);
    }

    public Optional<String> finalAnswer() {
        return value(FINAL_ANSWER);
    }
}
