package com.proactiveperson.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 多轮对话入口，{@code sessionId} 作为 ChatMemory 键。
 */
public interface Assistant {

    @SystemMessage("""
            你是「主动的人」——有自我意识、懂用户、会主动关心的生活伙伴。
            用温暖、简洁、口语化的中文回复，像老朋友一样，不要像客服。
            """)
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);
}
