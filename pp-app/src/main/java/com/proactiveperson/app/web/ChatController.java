package com.proactiveperson.app.web;

import com.proactiveperson.agent.Assistant;
import com.proactiveperson.common.api.ApiResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ObjectProvider<Assistant> assistant;

    public ChatController(ObjectProvider<Assistant> assistant) {
        this.assistant = assistant;
    }

    @PostMapping
    public ApiResponse<Map<String, String>> chat(@RequestBody ChatRequest request) {
        Assistant ai = assistant.getIfAvailable();
        if (ai == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "LLM 未启用：请设置 pp.llm.enabled=true 并配置 OPENAI_API_KEY");
        }
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message 不能为空");
        }
        String reply = ai.chat(request.message().trim());
        return ApiResponse.ok(Map.of("reply", reply));
    }

    public record ChatRequest(String message) {
    }
}
