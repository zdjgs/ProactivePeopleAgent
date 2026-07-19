package com.proactiveperson.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pp.agent.graph")
public class AgentGraphRuntimeProperties {

    /** Supervisor 图最大迭代次数（防死循环） */
    private int maxIterations = 5;

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }
}
