package com.proactiveperson.wechat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pp.wechat")
public class WeChatProperties {

    /** stub | official */
    private String provider = "stub";

    private String appId = "";
    private String appSecret = "";
    /** 服务器配置 Token，用于回调验签 */
    private String token = "";
    private String apiBaseUrl = "https://api.weixin.qq.com";
    private int timeoutSeconds = 10;

    /** 客服消息互动窗口（小时），微信官方为 48 */
    private int customerServiceWindowHours = 48;

    /** 超窗降级：模板消息 template_id（未配置则仅记录失败原因） */
    private String templateId = "";
    /** 模板中承载正文的 data key，如 thing1 / content */
    private String templateContentKey = "thing1";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getCustomerServiceWindowHours() {
        return customerServiceWindowHours;
    }

    public void setCustomerServiceWindowHours(int customerServiceWindowHours) {
        this.customerServiceWindowHours = customerServiceWindowHours;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateContentKey() {
        return templateContentKey;
    }

    public void setTemplateContentKey(String templateContentKey) {
        this.templateContentKey = templateContentKey;
    }
}
