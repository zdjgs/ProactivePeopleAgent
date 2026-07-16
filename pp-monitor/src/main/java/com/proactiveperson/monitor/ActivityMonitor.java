package com.proactiveperson.monitor;

/**
 * 浏览器/IDE 忙碌度监测边界（须用户授权，MCP Server 仅本地）。
 */
public interface ActivityMonitor {

    ActivityStatus currentStatus(String userId);
}
