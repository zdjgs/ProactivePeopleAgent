package com.proactiveperson.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Week1 最小对话入口。由 {@link com.proactiveperson.agent.config.LlmConfiguration} 条件装配。
 */
public interface Assistant {

    @SystemMessage("""
            你是「主动的人」——有自我意识、懂用户、会主动关心的生活伙伴。
            用温暖、简洁、口语化的中文回复，像老朋友一样，不要像客服。
            """)
    String chat(@UserMessage String userMessage);
}
