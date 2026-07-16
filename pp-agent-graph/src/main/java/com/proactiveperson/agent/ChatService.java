package com.proactiveperson.agent;

/**
 * 对话应用服务：多轮记忆 + 短期层落库边界。
 */
public interface ChatService {

    ChatResult chat(String userId, String sessionId, String message);

    record ChatResult(String sessionId, String reply) {
    }
}
