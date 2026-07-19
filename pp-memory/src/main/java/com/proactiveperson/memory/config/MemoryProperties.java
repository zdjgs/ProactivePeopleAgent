package com.proactiveperson.memory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pp.memory")
public class MemoryProperties {

    /** stub | mem0 */
    private String provider = "stub";

    private final Mem0 mem0 = new Mem0();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Mem0 getMem0() {
        return mem0;
    }

    public static class Mem0 {

        /**
         * oss：自托管 Python REST Server（POST /memories、POST /search）
         * platform：Mem0 云服务（POST /v3/memories/add/、POST /v3/memories/search/）
         */
        private String mode = "oss";

        private String baseUrl = "http://localhost:8000";

        /** Platform 模式必填；OSS 可选 */
        private String apiKey = "";

        private int timeoutSeconds = 10;

        /**
         * 写入原始对话轮次时关闭 LLM 推断，避免把整段对话拆成多条事实。
         */
        private boolean inferOnAdd = false;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public boolean isInferOnAdd() {
            return inferOnAdd;
        }

        public void setInferOnAdd(boolean inferOnAdd) {
            this.inferOnAdd = inferOnAdd;
        }
    }
}
