package com.proactiveperson.wechat.inbound;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析微信明文模式 XML（不引入重量级 XML 库；加密模式留到后续）。
 */
@Component
public class WeChatXmlMessageParser {

    private static final Pattern FROM_USER = Pattern.compile("<FromUserName><!\\[CDATA\\[(.+?)]]></FromUserName>");
    private static final Pattern MSG_TYPE = Pattern.compile("<MsgType><!\\[CDATA\\[(.+?)]]></MsgType>");
    private static final Pattern CONTENT = Pattern.compile("<Content><!\\[CDATA\\[(.+?)]]></Content>");
    private static final Pattern CREATE_TIME = Pattern.compile("<CreateTime>(\\d+)</CreateTime>");

    public Optional<InboundWeChatMessage> parse(String xml) {
        if (!StringUtils.hasText(xml)) {
            return Optional.empty();
        }
        String openId = match(FROM_USER, xml);
        String msgType = match(MSG_TYPE, xml);
        if (!StringUtils.hasText(openId) || !StringUtils.hasText(msgType)) {
            return Optional.empty();
        }
        String content = match(CONTENT, xml);
        Instant createTime = parseCreateTime(match(CREATE_TIME, xml));
        return Optional.of(new InboundWeChatMessage(openId, msgType, content, createTime));
    }

    private static Instant parseCreateTime(String epochSeconds) {
        if (!StringUtils.hasText(epochSeconds)) {
            return Instant.now();
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(epochSeconds));
        } catch (NumberFormatException ex) {
            return Instant.now();
        }
    }

    private static String match(Pattern pattern, String xml) {
        Matcher matcher = pattern.matcher(xml);
        return matcher.find() ? matcher.group(1) : null;
    }
}
