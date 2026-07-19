package com.proactiveperson.wechat.official;

import com.proactiveperson.common.exception.WeChatInvocationException;
import com.proactiveperson.wechat.WeChatMessageGateway;
import com.proactiveperson.wechat.WeChatOutboundChannel;
import com.proactiveperson.wechat.api.WeChatOfficialApiClient;
import com.proactiveperson.wechat.config.WeChatProperties;
import com.proactiveperson.wechat.window.CustomerServiceWindowTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@ConditionalOnProperty(name = "pp.wechat.provider", havingValue = "official")
public class OfficialWeChatMessageGateway implements WeChatMessageGateway {

    private static final Logger log = LoggerFactory.getLogger(OfficialWeChatMessageGateway.class);

    private final WeChatOfficialApiClient apiClient;
    private final CustomerServiceWindowTracker windowTracker;
    private final WeChatProperties properties;

    public OfficialWeChatMessageGateway(WeChatOfficialApiClient apiClient,
                                        CustomerServiceWindowTracker windowTracker,
                                        WeChatProperties properties) {
        this.apiClient = apiClient;
        this.windowTracker = windowTracker;
        this.properties = properties;
    }

    @Override
    public SendResult sendCustomerText(String openId, String content) {
        validate(openId, content);
        try {
            String messageId = apiClient.sendCustomerText(openId, content);
            log.info("wechat customer text sent openId={} messageId={} sentAt={} contentLength={}",
                    openId, messageId, Instant.now(), content.length());
            return SendResult.ok(WeChatOutboundChannel.CUSTOMER_SERVICE, messageId);
        } catch (WeChatInvocationException ex) {
            log.warn("wechat customer text failed openId={} detail={}", openId, ex.getMessage());
            return SendResult.fail(WeChatOutboundChannel.CUSTOMER_SERVICE, ex.getMessage());
        }
    }

    @Override
    public SendResult sendTemplateText(String openId, String content) {
        validate(openId, content);
        try {
            String messageId = apiClient.sendTemplateText(openId, content);
            log.info("wechat template text sent openId={} messageId={} sentAt={} templateId={}",
                    openId, messageId, Instant.now(), properties.getTemplateId());
            return SendResult.ok(WeChatOutboundChannel.TEMPLATE, messageId);
        } catch (WeChatInvocationException ex) {
            log.warn("wechat template text failed openId={} detail={}", openId, ex.getMessage());
            return SendResult.fail(WeChatOutboundChannel.TEMPLATE, ex.getMessage());
        }
    }

    @Override
    public SendResult sendTextAuto(String openId, String content) {
        WeChatOutboundChannel channel = resolveChannel(openId);
        if (channel == WeChatOutboundChannel.CUSTOMER_SERVICE) {
            return sendCustomerText(openId, content);
        }
        if (!StringUtils.hasText(properties.getTemplateId())) {
            String detail = "超出 " + properties.getCustomerServiceWindowHours()
                    + "h 客服窗口且未配置 template-id；请申请模板消息或引导用户主动发消息打开窗口（订阅消息见 T-009）";
            log.warn("wechat auto-send blocked openId={} detail={}", openId, detail);
            return SendResult.fail(WeChatOutboundChannel.TEMPLATE, detail);
        }
        return sendTemplateText(openId, content);
    }

    @Override
    public WeChatOutboundChannel resolveChannel(String openId) {
        return windowTracker.isWithinCustomerServiceWindow(openId)
                ? WeChatOutboundChannel.CUSTOMER_SERVICE
                : WeChatOutboundChannel.TEMPLATE;
    }

    private static void validate(String openId, String content) {
        if (!StringUtils.hasText(openId)) {
            throw new WeChatInvocationException("openId 不能为空");
        }
        if (!StringUtils.hasText(content)) {
            throw new WeChatInvocationException("消息内容不能为空");
        }
    }
}
