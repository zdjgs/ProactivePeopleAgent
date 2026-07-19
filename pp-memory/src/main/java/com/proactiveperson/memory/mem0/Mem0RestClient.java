package com.proactiveperson.memory.mem0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.common.exception.MemoryInvocationException;
import com.proactiveperson.common.util.SensitiveDataMasker;
import com.proactiveperson.memory.config.MemoryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mem0 HTTP 客户端：支持自托管 OSS 与 Platform 两套路径。
 * 官方无 Java SDK，按 REST 文档封装。
 */
public class Mem0RestClient {

    private static final Logger log = LoggerFactory.getLogger(Mem0RestClient.class);

    private final MemoryProperties.Mem0 config;
    private final Mem0ApiMode mode;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public Mem0RestClient(MemoryProperties properties) {
        this.config = properties.getMem0();
        this.mode = Mem0ApiMode.fromConfig(config.getMode());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
        this.objectMapper = new ObjectMapper();
        validateConfig();
    }

    public void add(String userId, MemoryLayer layer, String content, Map<String, String> extraMetadata) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(Mem0LayerMetadata.LAYER_KEY, Mem0LayerMetadata.layerValue(layer));
        if (extraMetadata != null) {
            metadata.putAll(extraMetadata);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user_id", userId);
        body.put("messages", parseMessages(content));
        body.put("metadata", metadata);
        body.put("infer", config.isInferOnAdd());

        String path = mode == Mem0ApiMode.PLATFORM ? "/v3/memories/add/" : "/memories";
        post(path, body);
    }

    public List<String> search(String userId, MemoryLayer layer, String query, int limit) {
        Map<String, Object> body = buildSearchBody(userId, layer, query, limit);
        String path = mode == Mem0ApiMode.PLATFORM ? "/v3/memories/search/" : "/search";
        JsonNode root = post(path, body);
        return extractMemories(root, layer, limit);
    }

    private Map<String, Object> buildSearchBody(String userId, MemoryLayer layer, String query, int limit) {
        if (mode == Mem0ApiMode.PLATFORM) {
            Map<String, Object> filters = new LinkedHashMap<>();
            filters.put("user_id", userId);
            filters.put("metadata", Map.of(Mem0LayerMetadata.LAYER_KEY, Mem0LayerMetadata.layerValue(layer)));

            Map<String, Object> body = new LinkedHashMap<>();
            if (StringUtils.hasText(query)) {
                body.put("query", query);
            }
            body.put("filters", filters);
            body.put("limit", limit);
            return body;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user_id", userId);
        if (StringUtils.hasText(query)) {
            body.put("query", query);
        }
        body.put("limit", limit);
        body.put("metadata", Map.of(Mem0LayerMetadata.LAYER_KEY, Mem0LayerMetadata.layerValue(layer)));
        return body;
    }

    private JsonNode post(String path, Map<String, Object> body) {
        String url = normalizeBaseUrl(config.getBaseUrl()) + path;
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            if (StringUtils.hasText(config.getApiKey())) {
                builder.header("Authorization", "Token " + config.getApiKey());
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("mem0 http {} status={} body={}", path, response.statusCode(), truncate(response.body()));
                throw new MemoryInvocationException("Mem0 请求失败 HTTP " + response.statusCode());
            }
            if (!StringUtils.hasText(response.body())) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(response.body());
        } catch (MemoryInvocationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("mem0 http {} failed: {}", path, ex.getMessage());
            throw new MemoryInvocationException("Mem0 调用异常: " + ex.getMessage(), ex);
        }
    }

    private List<String> extractMemories(JsonNode root, MemoryLayer layer, int limit) {
        JsonNode items = root;
        if (root.has("results")) {
            items = root.get("results");
        }
        if (!items.isArray()) {
            return List.of();
        }

        String layerValue = Mem0LayerMetadata.layerValue(layer);
        List<String> memories = new ArrayList<>();
        for (JsonNode item : items) {
            String memoryText = readMemoryText(item);
            if (!StringUtils.hasText(memoryText)) {
                continue;
            }
            if (!matchesLayer(item, layerValue)) {
                continue;
            }
            memories.add(memoryText);
            if (memories.size() >= limit) {
                break;
            }
        }
        return memories;
    }

    private static boolean matchesLayer(JsonNode item, String layerValue) {
        JsonNode metadata = item.get("metadata");
        if (metadata == null || metadata.isNull()) {
            return false;
        }
        JsonNode layerNode = metadata.get(Mem0LayerMetadata.LAYER_KEY);
        if (layerNode == null || layerNode.isNull()) {
            return false;
        }
        return layerValue.equals(layerNode.asText());
    }

    private static String readMemoryText(JsonNode item) {
        if (item.hasNonNull("memory")) {
            return item.get("memory").asText();
        }
        if (item.hasNonNull("text")) {
            return item.get("text").asText();
        }
        if (item.hasNonNull("content")) {
            return item.get("content").asText();
        }
        return null;
    }

    static List<Map<String, String>> parseMessages(String content) {
        String userPart = null;
        String assistantPart = null;
        for (String line : content.split("\n")) {
            if (line.startsWith("user: ")) {
                userPart = line.substring("user: ".length());
            } else if (line.startsWith("assistant: ")) {
                assistantPart = line.substring("assistant: ".length());
            }
        }
        List<Map<String, String>> messages = new ArrayList<>();
        if (StringUtils.hasText(userPart)) {
            messages.add(Map.of("role", "user", "content", userPart));
        }
        if (StringUtils.hasText(assistantPart)) {
            messages.add(Map.of("role", "assistant", "content", assistantPart));
        }
        if (messages.isEmpty()) {
            messages.add(Map.of("role", "user", "content", content));
        }
        return messages;
    }

    private void validateConfig() {
        if (!StringUtils.hasText(config.getBaseUrl())) {
            throw new IllegalStateException("pp.memory.mem0.base-url 未配置");
        }
        if (mode == Mem0ApiMode.PLATFORM && !StringUtils.hasText(config.getApiKey())) {
            throw new IllegalStateException("Mem0 Platform 模式需要配置 pp.memory.mem0.api-key / MEM0_API_KEY");
        }
        log.info("Mem0RestClient ready mode={} baseUrl={} apiKey={}",
                mode, config.getBaseUrl(), SensitiveDataMasker.maskSecret(config.getApiKey()));
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}
