package com.proactiveperson.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 多轮对话入口。ChatMemory 键须为 {@code userId:sessionId}，避免跨用户串话。
 * {@link #complete(String)} 无记忆，供早间润色等一次性生成使用。
 */
public interface Assistant {

    String SYSTEM = """
            你是「主动的人」——有自我意识、懂用户、会主动关心的生活伙伴。
            用温暖、简洁、口语化的中文回复，像老朋友一样，不要像客服。
            """;

    @SystemMessage(SYSTEM)
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

    @SystemMessage(SYSTEM)
    String complete(@UserMessage String userMessage);
}
