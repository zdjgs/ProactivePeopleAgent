package com.proactiveperson.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 外部基础设施占位配置。默认全部 disabled，未启用时不影响应用启动。
 */
@ConfigurationProperties(prefix = "pp.infra")
public class InfraProperties {

    private final Postgres postgres = new Postgres();
    private final Redis redis = new Redis();
    private final Mem0 mem0 = new Mem0();

    public Postgres getPostgres() {
        return postgres;
    }

    public Redis getRedis() {
        return redis;
    }

    public Mem0 getMem0() {
        return mem0;
    }

    public static class Postgres {
        private boolean enabled = false;
        private String url = "jdbc:postgresql://localhost:5432/pp_agent";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class Redis {
        private boolean enabled = false;
        private String host = "localhost";
        private int port = 6379;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class Mem0 {
        private boolean enabled = false;
        private String baseUrl = "http://localhost:8000";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
