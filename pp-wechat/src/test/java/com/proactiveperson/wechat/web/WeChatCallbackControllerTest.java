package com.proactiveperson.wechat.web;

import com.proactiveperson.common.state.InMemoryStateStore;
import com.proactiveperson.wechat.config.WeChatProperties;
import com.proactiveperson.wechat.inbound.WeChatXmlMessageParser;
import com.proactiveperson.wechat.security.WeChatSignatureVerifier;
import com.proactiveperson.wechat.window.CustomerServiceWindowTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeChatCallbackControllerTest {

    private WeChatProperties properties;
    private CustomerServiceWindowTracker windowTracker;
    private WeChatCallbackController controller;
    private Clock clock;

    @BeforeEach
    void setUp() {
        properties = new WeChatProperties();
        properties.setToken("ppToken");
        properties.setCallbackTimestampMaxSkewSeconds(300);
        clock = Clock.fixed(Instant.parse("2026-07-19T08:00:00Z"), ZoneOffset.UTC);
        windowTracker = CustomerServiceWindowTracker.createForTest(
                properties, new InMemoryStateStore(), clock);
        controller = new WeChatCallbackController(
                properties,
                WeChatSignatureVerifier.createForTest(properties, clock),
                new WeChatXmlMessageParser(),
                windowTracker,
                List.of());
    }

    @Test
    void verifyReturnsEchostrWhenSignatureValid() {
        String timestamp = String.valueOf(clock.instant().getEpochSecond());
        String nonce = "n1";
        String signature = WeChatSignatureVerifier.sha1Hex(
                WeChatSignatureVerifier.sortedConcat("ppToken", timestamp, nonce));

        ResponseEntity<String> response = controller.verify(signature, timestamp, nonce, "echo-ok");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("echo-ok");
    }

    @Test
    void receiveUpdates48hWindow() {
        String timestamp = String.valueOf(clock.instant().getEpochSecond());
        String nonce = "n2";
        String signature = WeChatSignatureVerifier.sha1Hex(
                WeChatSignatureVerifier.sortedConcat("ppToken", timestamp, nonce));
        long createTime = clock.instant().getEpochSecond();
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

    @Test
    void rejectsStaleTimestamp() {
        String timestamp = String.valueOf(clock.instant().getEpochSecond() - 3600);
        String nonce = "n3";
        String signature = WeChatSignatureVerifier.sha1Hex(
                WeChatSignatureVerifier.sortedConcat("ppToken", timestamp, nonce));

        ResponseEntity<String> response = controller.verify(signature, timestamp, nonce, "echo");

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }
}
