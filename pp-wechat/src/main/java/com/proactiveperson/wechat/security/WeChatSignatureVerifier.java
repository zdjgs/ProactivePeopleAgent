package com.proactiveperson.wechat.security;

import com.proactiveperson.wechat.config.WeChatProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;

/**
 * 微信服务器配置 URL 验签：sha1(sort(token, timestamp, nonce))，并校验 timestamp 防重放。
 */
@Component
public class WeChatSignatureVerifier {

    private final WeChatProperties properties;
    private final Clock clock;

    @Autowired
    public WeChatSignatureVerifier(WeChatProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public static WeChatSignatureVerifier createForTest(WeChatProperties properties, Clock clock) {
        return new WeChatSignatureVerifier(properties, clock);
    }

    private WeChatSignatureVerifier(WeChatProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** 兼容旧单测：无配置时不做 skew 校验。 */
    public WeChatSignatureVerifier() {
        this(defaultProps(0), Clock.systemUTC());
    }

    public boolean verify(String token, String signature, String timestamp, String nonce) {
        if (!StringUtils.hasText(token)
                || !StringUtils.hasText(signature)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce)) {
            return false;
        }
        if (!isTimestampFresh(timestamp)) {
            return false;
        }
        String expected = sha1Hex(sortedConcat(token, timestamp, nonce));
        return expected.equalsIgnoreCase(signature);
    }

    private boolean isTimestampFresh(String timestamp) {
        int maxSkew = properties.getCallbackTimestampMaxSkewSeconds();
        if (maxSkew <= 0) {
            return true;
        }
        try {
            long ts = Long.parseLong(timestamp);
            long now = Instant.now(clock).getEpochSecond();
            return Math.abs(now - ts) <= maxSkew;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static String sortedConcat(String token, String timestamp, String nonce) {
        String[] arr = {token, timestamp, nonce};
        Arrays.sort(arr);
        return arr[0] + arr[1] + arr[2];
    }

    public static String sha1Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }

    private static WeChatProperties defaultProps(int skewSeconds) {
        WeChatProperties props = new WeChatProperties();
        props.setCallbackTimestampMaxSkewSeconds(skewSeconds);
        return props;
    }
}
