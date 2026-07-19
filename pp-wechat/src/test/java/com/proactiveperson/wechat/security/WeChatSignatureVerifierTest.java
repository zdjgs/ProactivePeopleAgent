package com.proactiveperson.wechat.security;

import com.proactiveperson.wechat.config.WeChatProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class WeChatSignatureVerifierTest {

    @Test
    void verifiesOfficialExampleStyleSignature() {
        WeChatProperties props = new WeChatProperties();
        props.setCallbackTimestampMaxSkewSeconds(0); // 仅校验签名
        WeChatSignatureVerifier verifier = WeChatSignatureVerifier.createForTest(
                props, Clock.systemUTC());

        String token = "ppToken";
        String timestamp = "1710000000";
        String nonce = "nonce123";
        String signature = WeChatSignatureVerifier.sha1Hex(
                WeChatSignatureVerifier.sortedConcat(token, timestamp, nonce));

        assertThat(verifier.verify(token, signature, timestamp, nonce)).isTrue();
        assertThat(verifier.verify(token, "deadbeef", timestamp, nonce)).isFalse();
    }

    @Test
    void rejectsMissingParams() {
        WeChatSignatureVerifier verifier = new WeChatSignatureVerifier();
        assertThat(verifier.verify("t", null, "1", "n")).isFalse();
        assertThat(verifier.verify("", "sig", "1", "n")).isFalse();
    }

    @Test
    void rejectsSkewedTimestampWhenEnabled() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T08:00:00Z"), ZoneOffset.UTC);
        WeChatProperties props = new WeChatProperties();
        props.setCallbackTimestampMaxSkewSeconds(300);
        WeChatSignatureVerifier verifier = WeChatSignatureVerifier.createForTest(props, clock);

        String token = "ppToken";
        String timestamp = String.valueOf(clock.instant().getEpochSecond() - 1000);
        String nonce = "n";
        String signature = WeChatSignatureVerifier.sha1Hex(
                WeChatSignatureVerifier.sortedConcat(token, timestamp, nonce));

        assertThat(verifier.verify(token, signature, timestamp, nonce)).isFalse();
    }
}
