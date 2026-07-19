package com.proactiveperson.proactive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pp.disturbance")
public class DisturbanceProperties {

    /** memory | redis */
    private String cacheProvider = "memory";

    private int cacheTtlSeconds = 300;

    /** lettuce URI，如 redis://localhost:6379/0 */
    private String redisUri = "redis://localhost:6379/0";

    /** 消息末尾快捷回复提示 */
    private boolean appendMuteHint = true;

    private String muteHint = "回复「今天别打扰」可暂停今日主动消息。";

    public String getCacheProvider() {
        return cacheProvider;
    }

    public void setCacheProvider(String cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public String getRedisUri() {
        return redisUri;
    }

    public void setRedisUri(String redisUri) {
        this.redisUri = redisUri;
    }

    public boolean isAppendMuteHint() {
        return appendMuteHint;
    }

    public void setAppendMuteHint(boolean appendMuteHint) {
        this.appendMuteHint = appendMuteHint;
    }

    public String getMuteHint() {
        return muteHint;
    }

    public void setMuteHint(String muteHint) {
        this.muteHint = muteHint;
    }
}
