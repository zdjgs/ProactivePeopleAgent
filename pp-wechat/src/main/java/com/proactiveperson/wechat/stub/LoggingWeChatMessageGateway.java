package com.proactiveperson.wechat.stub;

import com.proactiveperson.wechat.WeChatMessageGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "pp.wechat.provider", havingValue = "stub", matchIfMissing = true)
public class LoggingWeChatMessageGateway implements WeChatMessageGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingWeChatMessageGateway.class);

    @Override
    public SendResult sendCustomerText(String openId, String content) {
        String messageId = UUID.randomUUID().toString();
        log.info("stub wechat customer message openId={} messageId={} sentAt={} contentLength={}",
                openId, messageId, Instant.now(), content == null ? 0 : content.length());
        return SendResult.ok(messageId);
    }
}
