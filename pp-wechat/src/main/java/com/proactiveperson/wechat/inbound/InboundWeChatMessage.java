package com.proactiveperson.wechat.inbound;

import java.time.Instant;
import java.util.Optional;

/**
 * 微信服务器推送的入站消息（明文 XML 解析结果）。
 */
public record InboundWeChatMessage(
        String openId,
        String msgType,
        String content,
        Instant createTime
) {

    public boolean isText() {
        return "text".equalsIgnoreCase(msgType);
    }

    public Optional<String> textContent() {
        return isText() && content != null && !content.isBlank()
                ? Optional.of(content)
                : Optional.empty();
    }
}
