package com.proactiveperson.agent;

import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.common.exception.LlmInvocationException;
import com.proactiveperson.common.exception.MemoryInvocationException;
import com.proactiveperson.memory.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(name = "pp.llm.enabled", havingValue = "true")
public class DefaultChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(DefaultChatService.class);

    private final Assistant assistant;
    private final MemoryService memoryService;

    public DefaultChatService(Assistant assistant, MemoryService memoryService) {
        this.assistant = assistant;
        this.memoryService = memoryService;
    }

    @Override
    public ChatResult chat(String userId, String sessionId, String message) {
        String resolvedUserId = StringUtils.hasText(userId) ? userId : "anonymous";
        String memoryId = memoryId(resolvedUserId, sessionId);
        try {
            String reply = assistant.chat(memoryId, message);
            persistShortTermMemory(resolvedUserId, message, reply);
            log.debug("chat completed userId={} sessionId={} replyLength={}",
                    resolvedUserId, sessionId, reply == null ? 0 : reply.length());
            return new ChatResult(sessionId, reply == null ? "" : reply);
        } catch (RuntimeException ex) {
            log.warn("llm invocation failed userId={} sessionId={} cause={}",
                    resolvedUserId, sessionId, ex.getMessage());
            throw new LlmInvocationException("模型调用失败，请稍后重试", ex);
        }
    }

    static String memoryId(String userId, String sessionId) {
        return userId + ":" + sessionId;
    }

    private void persistShortTermMemory(String userId, String message, String reply) {
        try {
            memoryService.add(userId, MemoryLayer.SHORT_TERM, formatTurn(message, reply));
        } catch (MemoryInvocationException ex) {
            log.warn("short-term memory persist failed userId={} cause={}", userId, ex.getMessage());
        }
    }

    private static String formatTurn(String userMessage, String assistantReply) {
        return "user: " + userMessage + "\nassistant: " + assistantReply;
    }
}
