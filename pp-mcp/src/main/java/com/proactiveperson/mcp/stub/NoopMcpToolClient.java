package com.proactiveperson.mcp.stub;

import com.proactiveperson.mcp.McpToolClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Stub MCP：为早间推送提供可演示的天气/新闻摘要；未识别工具返回 stub 状态。
 */
@Service
@ConditionalOnProperty(name = "pp.mcp.provider", havingValue = "stub", matchIfMissing = true)
public class NoopMcpToolClient implements McpToolClient {

    @Override
    public List<String> listTools() {
        return List.of("weather", "news");
    }

    @Override
    public Object callTool(String toolName, Map<String, Object> arguments) {
        if ("weather".equalsIgnoreCase(toolName)) {
            String city = String.valueOf(arguments.getOrDefault("city", "通用"));
            if ("通用".equals(city)) {
                return Map.of("summary", "今天天气平和，适合按自己的节奏安排。");
            }
            return Map.of("summary", city + "今天天气不错，适合出门走走或办点小事。");
        }
        if ("news".equalsIgnoreCase(toolName)) {
            return Map.of("summary", "外面世界还在转，但你的节奏由你决定。");
        }
        return Map.of("tool", toolName, "status", "stub-not-configured");
    }
}
