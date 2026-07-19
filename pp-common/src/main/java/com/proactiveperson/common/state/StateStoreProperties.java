package com.proactiveperson.common.state;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pp.state")
public class StateStoreProperties {

    /** memory | redis */
    private String store = "memory";

    /** lettuce URI，如 redis://localhost:6379/0 */
    private String redisUri = "redis://localhost:6379/0";

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public String getRedisUri() {
        return redisUri;
    }

    public void setRedisUri(String redisUri) {
        this.redisUri = redisUri;
    }

    public boolean isRedis() {
        return "redis".equalsIgnoreCase(store);
    }
}
