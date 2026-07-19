package com.proactiveperson.app.web;

import com.proactiveperson.agent.graph.AgentGraphService;
import com.proactiveperson.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentGraphService agentGraphService;

    public AgentController(AgentGraphService agentGraphService) {
        this.agentGraphService = agentGraphService;
    }

    @PostMapping("/run")
    public ApiResponse<Map<String, Object>> run(@Valid @RequestBody AgentRunRequest request) {
        AgentGraphService.AgentGraphResult result = agentGraphService.run(request.userId(), request.query());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", result.userId());
        body.put("query", result.query());
        body.put("answer", result.answer());
        body.put("iterations", result.iterations());
        body.put("researchNotes", result.researchNotes());
        body.put("personaNotes", result.personaNotes());
        return ApiResponse.ok(body);
    }

    public record AgentRunRequest(
            String userId,
            @NotBlank(message = "query 不能为空") String query
    ) {
    }
}
