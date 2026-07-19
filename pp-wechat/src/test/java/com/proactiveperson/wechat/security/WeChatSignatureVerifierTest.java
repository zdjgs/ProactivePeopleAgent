package com.proactiveperson.wechat.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeChatSignatureVerifierTest {

    private final WeChatSignatureVerifier verifier = new WeChatSignatureVerifier();

    @Test
    void verifiesOfficialExampleStyleSignature() {
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
        assertThat(verifier.verify("t", null, "1", "n")).isFalse();
        assertThat(verifier.verify("", "sig", "1", "n")).isFalse();
    }
}
