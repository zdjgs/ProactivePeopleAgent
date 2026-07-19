package com.proactiveperson.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pp.api")
public class ApiSecurityProperties {

    /** 默认关闭，本地联调免配 Key；生产务必开启 */
    private boolean authEnabled = false;

    /** 与请求头 X-API-Key 比对 */
    private String token = "";

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public void setAuthEnabled(boolean authEnabled) {
        this.authEnabled = authEnabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
