package com.proactiveperson.app.web;

import com.proactiveperson.agent.ChatService;
import com.proactiveperson.common.api.ApiResponse;
import com.proactiveperson.common.exception.LlmNotEnabledException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ObjectProvider<ChatService> chatService;

    public ChatController(ObjectProvider<ChatService> chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ApiResponse<Map<String, String>> chat(@Valid @RequestBody ChatRequest request) {
        ChatService service = chatService.getIfAvailable();
        if (service == null) {
            throw new LlmNotEnabledException();
        }
        ChatService.ChatResult result = service.chat(request.userId(), request.sessionId(), request.message());
        Map<String, String> body = new LinkedHashMap<>();
        body.put("sessionId", result.sessionId());
        body.put("reply", result.reply() == null ? "" : result.reply());
        return ApiResponse.ok(body);
    }

    public record ChatRequest(
            @NotBlank(message = "sessionId 不能为空") String sessionId,
            @NotBlank(message = "message 不能为空") String message,
            String userId
    ) {
    }
}
