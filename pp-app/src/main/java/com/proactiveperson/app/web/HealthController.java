package com.proactiveperson.app.web;

import com.proactiveperson.agent.Assistant;
import com.proactiveperson.agent.config.AgentGraphProperties;
import com.proactiveperson.common.api.ApiResponse;
import com.proactiveperson.proactive.config.ProactiveProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final AgentGraphProperties llmProperties;
    private final ProactiveProperties proactiveProperties;
    private final ObjectProvider<Assistant> assistant;

    public HealthController(AgentGraphProperties llmProperties,
                            ProactiveProperties proactiveProperties,
                            ObjectProvider<Assistant> assistant) {
        this.llmProperties = llmProperties;
        this.proactiveProperties = proactiveProperties;
        this.assistant = assistant;
    }

    @GetMapping({"/", "/api/health"})
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", "ProactivePeopleAgent");
        body.put("status", "UP");
        body.put("llmEnabled", llmProperties.isEnabled());
        body.put("assistantReady", assistant.getIfAvailable() != null);
        body.put("morningPushEnabled", proactiveProperties.isMorningPushEnabled());
        body.put("dailyPushLimit", proactiveProperties.getDailyPushLimit());
        return ApiResponse.ok(body);
    }
}
