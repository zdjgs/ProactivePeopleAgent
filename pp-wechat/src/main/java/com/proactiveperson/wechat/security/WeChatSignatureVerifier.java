package com.proactiveperson.wechat.security;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 微信服务器配置 URL 验签：sha1(sort(token, timestamp, nonce))。
 */
@Component
public class WeChatSignatureVerifier {

    public boolean verify(String token, String signature, String timestamp, String nonce) {
        if (!StringUtils.hasText(token)
                || !StringUtils.hasText(signature)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce)) {
            return false;
        }
        String expected = sha1Hex(sortedConcat(token, timestamp, nonce));
        return expected.equalsIgnoreCase(signature);
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
}
