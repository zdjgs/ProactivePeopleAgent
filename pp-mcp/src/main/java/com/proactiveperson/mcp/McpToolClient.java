package com.proactiveperson.mcp;

import java.util.List;
import java.util.Map;

/**
 * MCP 动态工具客户端边界（Week3 接真实 MCP Server）。
 */
public interface McpToolClient {

    List<String> listTools();

    Object callTool(String toolName, Map<String, Object> arguments);
}
