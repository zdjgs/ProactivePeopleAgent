package com.proactiveperson.wechat.stub;

import com.proactiveperson.wechat.WeChatMessageGateway;
import com.proactiveperson.wechat.WeChatOutboundChannel;
import com.proactiveperson.wechat.config.WeChatProperties;
import com.proactiveperson.wechat.window.CustomerServiceWindowTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "pp.wechat.provider", havingValue = "stub", matchIfMissing = true)
public class LoggingWeChatMessageGateway implements WeChatMessageGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingWeChatMessageGateway.class);

    private final CustomerServiceWindowTracker windowTracker;
    private final WeChatProperties properties;

    public LoggingWeChatMessageGateway(CustomerServiceWindowTracker windowTracker,
                                       WeChatProperties properties) {
        this.windowTracker = windowTracker;
        this.properties = properties;
    }

    @Override
    public SendResult sendCustomerText(String openId, String content) {
        String messageId = UUID.randomUUID().toString();
        log.info("stub wechat customer message openId={} messageId={} sentAt={} contentLength={}",
                openId, messageId, Instant.now(), content == null ? 0 : content.length());
        return SendResult.ok(WeChatOutboundChannel.CUSTOMER_SERVICE, messageId);
    }

    @Override
    public SendResult sendTemplateText(String openId, String content) {
        if (!StringUtils.hasText(properties.getTemplateId())) {
            return SendResult.fail(WeChatOutboundChannel.TEMPLATE, "stub: template-id 未配置");
        }
        String messageId = UUID.randomUUID().toString();
        log.info("stub wechat template message openId={} messageId={} sentAt={} templateId={}",
                openId, messageId, Instant.now(), properties.getTemplateId());
        return SendResult.ok(WeChatOutboundChannel.TEMPLATE, messageId);
    }

    @Override
    public SendResult sendTextAuto(String openId, String content) {
        WeChatOutboundChannel channel = resolveChannel(openId);
        if (channel == WeChatOutboundChannel.CUSTOMER_SERVICE) {
            return sendCustomerText(openId, content);
        }
        return sendTemplateText(openId, content);
    }

    @Override
    public WeChatOutboundChannel resolveChannel(String openId) {
        return windowTracker.isWithinCustomerServiceWindow(openId)
                ? WeChatOutboundChannel.CUSTOMER_SERVICE
                : WeChatOutboundChannel.TEMPLATE;
    }
}
