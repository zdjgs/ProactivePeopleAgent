package com.proactiveperson.wechat.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proactiveperson.common.exception.WeChatInvocationException;
import com.proactiveperson.common.util.SensitiveDataMasker;
import com.proactiveperson.wechat.config.WeChatProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 微信官方 HTTP API：access_token + 客服消息 + 模板消息。
 * token 失效（40001/42001）时清缓存并重试一次。
 */
@Component
@ConditionalOnProperty(name = "pp.wechat.provider", havingValue = "official")
public class WeChatOfficialApiClient {

    private static final Logger log = LoggerFactory.getLogger(WeChatOfficialApiClient.class);
    private static final Set<Integer> RETRYABLE_TOKEN_ERRORS = Set.of(40001, 42001);

    private final WeChatProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public WeChatOfficialApiClient(WeChatProperties properties) {
        this(properties,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .build(),
                new ObjectMapper());
    }

    /** 测试用工厂，避免 Spring 误选多参构造器 */
    public static WeChatOfficialApiClient createForTest(WeChatProperties properties, HttpClient httpClient) {
        return new WeChatOfficialApiClient(properties, httpClient, new ObjectMapper());
    }

    private WeChatOfficialApiClient(WeChatProperties properties, HttpClient httpClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public String getAccessToken() {
        return getAccessToken(false);
    }

    private String getAccessToken(boolean forceRefresh) {
        if (!forceRefresh) {
            CachedToken current = cachedToken.get();
            if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
                return current.token();
            }
        }
        synchronized (this) {
            if (!forceRefresh) {
                CachedToken current = cachedToken.get();
                if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
                    return current.token();
                }
            }
            CachedToken refreshed = fetchAccessToken();
            cachedToken.set(refreshed);
            return refreshed.token();
        }
    }

    public String sendCustomerText(String openId, String content) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openId);
        body.put("msgtype", "text");
        body.put("text", Map.of("content", content));
        JsonNode root = postJsonWithTokenRetry("/cgi-bin/message/custom/send", body);
        assertOk(root, "custom/send");
        return root.path("msgid").asText(openId + "-" + Instant.now().toEpochMilli());
    }

    public String sendTemplateText(String openId, String content) {
        if (!StringUtils.hasText(properties.getTemplateId())) {
            throw new WeChatInvocationException("未配置 pp.wechat.template-id，无法走模板消息降级");
        }
        Map<String, Object> dataItem = Map.of("value", truncateForTemplate(content));
        Map<String, Object> data = Map.of(properties.getTemplateContentKey(), dataItem);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openId);
        body.put("template_id", properties.getTemplateId());
        body.put("data", data);

        JsonNode root = postJsonWithTokenRetry("/cgi-bin/message/template/send", body);
        assertOk(root, "template/send");
        return root.path("msgid").asText(null);
    }

    private JsonNode postJsonWithTokenRetry(String path, Map<String, Object> body) {
        JsonNode root = postJson(path + "?access_token=" + encode(getAccessToken(false)), body);
        if (isRetryableTokenError(root)) {
            log.warn("wechat access_token invalid errcode={}, refreshing and retrying once",
                    root.path("errcode").asInt());
            cachedToken.set(null);
            root = postJson(path + "?access_token=" + encode(getAccessToken(true)), body);
        }
        return root;
    }

    private static boolean isRetryableTokenError(JsonNode root) {
        if (root == null || !root.has("errcode")) {
            return false;
        }
        return RETRYABLE_TOKEN_ERRORS.contains(root.get("errcode").asInt());
    }

    private CachedToken fetchAccessToken() {
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getAppSecret())) {
            throw new WeChatInvocationException("微信官方模式需要配置 app-id / app-secret");
        }
        String path = "/cgi-bin/token?grant_type=client_credential"
                + "&appid=" + encode(properties.getAppId())
                + "&secret=" + encode(properties.getAppSecret());
        JsonNode root = getJson(path);
        if (root.has("errcode") && root.get("errcode").asInt() != 0) {
            throw new WeChatInvocationException("获取 access_token 失败: " + root.path("errmsg").asText());
        }
        String token = root.path("access_token").asText(null);
        int expiresIn = root.path("expires_in").asInt(7200);
        if (!StringUtils.hasText(token)) {
            throw new WeChatInvocationException("access_token 响应为空");
        }
        log.info("wechat access_token refreshed appId={} expiresIn={}s secret={}",
                properties.getAppId(), expiresIn, SensitiveDataMasker.maskSecret(properties.getAppSecret()));
        return new CachedToken(token, Instant.now().plusSeconds(expiresIn));
    }

    private JsonNode getJson(String path) {
        return exchange(HttpRequest.newBuilder()
                .uri(URI.create(normalizeBase(properties.getApiBaseUrl()) + path))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .GET()
                .build());
    }

    private JsonNode postJson(String path, Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            return exchange(HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBase(properties.getApiBaseUrl()) + path))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build());
        } catch (WeChatInvocationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WeChatInvocationException("微信请求序列化失败: " + ex.getMessage(), ex);
        }
    }

    private JsonNode exchange(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new WeChatInvocationException("微信 HTTP " + response.statusCode());
            }
            if (!StringUtils.hasText(response.body())) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(response.body());
        } catch (WeChatInvocationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WeChatInvocationException("微信调用异常: " + ex.getMessage(), ex);
        }
    }

    private static void assertOk(JsonNode root, String action) {
        if (root.has("errcode") && root.get("errcode").asInt() != 0) {
            throw new WeChatInvocationException(action + " 失败 errcode="
                    + root.get("errcode").asInt() + " errmsg=" + root.path("errmsg").asText());
        }
    }

    private static String truncateForTemplate(String content) {
        if (content == null) {
            return "";
        }
        return content.length() <= 20 ? content : content.substring(0, 19) + "…";
    }

    private static String normalizeBase(String base) {
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record CachedToken(String token, Instant expiresAt) {
    }
}
