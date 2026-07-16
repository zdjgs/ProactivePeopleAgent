package com.proactiveperson.app.web;

import com.proactiveperson.agent.ChatService;
import com.proactiveperson.agent.config.AgentGraphProperties;
import com.proactiveperson.app.config.InfraProperties;
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
    private final InfraProperties infraProperties;
    private final ObjectProvider<ChatService> chatService;

    public HealthController(AgentGraphProperties llmProperties,
                            ProactiveProperties proactiveProperties,
                            InfraProperties infraProperties,
                            ObjectProvider<ChatService> chatService) {
        this.llmProperties = llmProperties;
        this.proactiveProperties = proactiveProperties;
        this.infraProperties = infraProperties;
        this.chatService = chatService;
    }

    @GetMapping({"/", "/api/health"})
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", "ProactivePeopleAgent");
        body.put("status", "UP");
        body.put("llmEnabled", llmProperties.isEnabled());
        body.put("chatReady", chatService.getIfAvailable() != null);
        body.put("morningPushEnabled", proactiveProperties.isMorningPushEnabled());
        body.put("dailyPushLimit", proactiveProperties.getDailyPushLimit());
        body.put("morningWindow", proactiveProperties.getMorningWindowStartHour()
                + "-" + proactiveProperties.getMorningWindowEndHour());
        body.put("infra", Map.of(
                "postgresEnabled", infraProperties.getPostgres().isEnabled(),
                "redisEnabled", infraProperties.getRedis().isEnabled(),
                "mem0Enabled", infraProperties.getMem0().isEnabled()
        ));
        return ApiResponse.ok(body);
    }
}
