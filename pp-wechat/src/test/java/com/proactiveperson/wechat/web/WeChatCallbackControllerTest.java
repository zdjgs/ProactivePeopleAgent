package com.proactiveperson.wechat.web;

import com.proactiveperson.wechat.config.WeChatProperties;
import com.proactiveperson.wechat.inbound.WeChatXmlMessageParser;
import com.proactiveperson.wechat.security.WeChatSignatureVerifier;
import com.proactiveperson.wechat.window.CustomerServiceWindowTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WeChatCallbackControllerTest {

    private WeChatProperties properties;
    private CustomerServiceWindowTracker windowTracker;
    private WeChatCallbackController controller;

    @BeforeEach
    void setUp() {
        properties = new WeChatProperties();
        properties.setToken("ppToken");
        windowTracker = new CustomerServiceWindowTracker(properties);
        controller = new WeChatCallbackController(
                properties,
                new WeChatSignatureVerifier(),
                new WeChatXmlMessageParser(),
                windowTracker);
    }

    @Test
    void verifyReturnsEchostrWhenSignatureValid() {
        String timestamp = "1710000000";
        String nonce = "n1";
        String signature = WeChatSignatureVerifier.sha1Hex(
                WeChatSignatureVerifier.sortedConcat("ppToken", timestamp, nonce));

        ResponseEntity<String> response = controller.verify(signature, timestamp, nonce, "echo-ok");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("echo-ok");
    }

    @Test
    void receiveUpdates48hWindow() {
        String timestamp = "1710000000";
        String nonce = "n2";
        String signature = WeChatSignatureVerifier.sha1Hex(
                WeChatSignatureVerifier.sortedConcat("ppToken", timestamp, nonce));
        long createTime = Instant.now().getEpochSecond();
        String xml = """
                <xml>
                <FromUserName><![CDATA[user_openid]]></FromUserName>
                <CreateTime>%d</CreateTime>
                <MsgType><![CDATA[text]]></MsgType>
                <Content><![CDATA[你好]]></Content>
                </xml>
                """.formatted(createTime);

        ResponseEntity<String> response = controller.receive(signature, timestamp, nonce, xml);

        assertThat(response.getBody()).isEqualTo("success");
        assertThat(windowTracker.isWithinCustomerServiceWindow("user_openid")).isTrue();
        assertThat(windowTracker.lastInbound("user_openid")).isPresent();
    }
}
