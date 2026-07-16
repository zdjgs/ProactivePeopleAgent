package com.proactiveperson.mcp.stub;

import com.proactiveperson.mcp.McpToolClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "pp.mcp.provider", havingValue = "stub", matchIfMissing = true)
public class NoopMcpToolClient implements McpToolClient {

    @Override
    public List<String> listTools() {
        return List.of();
    }

    @Override
    public Object callTool(String toolName, Map<String, Object> arguments) {
        return Map.of("tool", toolName, "status", "stub-not-configured");
    }
}
