package com.proactiveperson.wechat.web;

import com.proactiveperson.wechat.config.WeChatProperties;
import com.proactiveperson.wechat.inbound.WeChatXmlMessageParser;
import com.proactiveperson.wechat.security.WeChatSignatureVerifier;
import com.proactiveperson.wechat.window.CustomerServiceWindowTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信服务器回调：URL 验签 + 入站消息（刷新 48h 客服窗口）。
 */
@RestController
@RequestMapping("/api/wechat/callback")
public class WeChatCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WeChatCallbackController.class);

    private final WeChatProperties properties;
    private final WeChatSignatureVerifier signatureVerifier;
    private final WeChatXmlMessageParser messageParser;
    private final CustomerServiceWindowTracker windowTracker;

    public WeChatCallbackController(WeChatProperties properties,
                                    WeChatSignatureVerifier signatureVerifier,
                                    WeChatXmlMessageParser messageParser,
                                    CustomerServiceWindowTracker windowTracker) {
        this.properties = properties;
        this.signatureVerifier = signatureVerifier;
        this.messageParser = messageParser;
        this.windowTracker = windowTracker;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {
        if (!signatureVerifier.verify(properties.getToken(), signature, timestamp, nonce)) {
            log.warn("wechat callback verify failed");
            return ResponseEntity.status(403).body("invalid signature");
        }
        return ResponseEntity.ok(echostr);
    }

    @PostMapping(consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.ALL_VALUE},
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> receive(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestBody String body) {
        if (!signatureVerifier.verify(properties.getToken(), signature, timestamp, nonce)) {
            log.warn("wechat inbound signature invalid");
            return ResponseEntity.status(403).body("invalid signature");
        }
        if (!StringUtils.hasText(properties.getToken())) {
            log.warn("wechat token empty — set pp.wechat.token before production");
        }

        messageParser.parse(body).ifPresentOrElse(msg -> {
            windowTracker.recordInbound(msg.openId(), msg.createTime());
            log.info("wechat inbound openId={} msgType={} createTime={} contentLength={}",
                    msg.openId(),
                    msg.msgType(),
                    msg.createTime(),
                    msg.content() == null ? 0 : msg.content().length());
            // 文本回复编排留给对话链路（ChatService / Agent）；此处只维护窗口与可观测日志
        }, () -> log.warn("wechat inbound xml parse empty"));

        return ResponseEntity.ok("success");
    }
}
