package com.proactiveperson.proactive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pp.proactive")
public class ProactiveProperties {

    /** 早间推送调度开关（Week1 默认关闭，避免空跑） */
    private boolean morningPushEnabled = false;

    /** 每日主动推送上限 */
    private int dailyPushLimit = 2;

    public boolean isMorningPushEnabled() {
        return morningPushEnabled;
    }

    public void setMorningPushEnabled(boolean morningPushEnabled) {
        this.morningPushEnabled = morningPushEnabled;
    }

    public int getDailyPushLimit() {
        return dailyPushLimit;
    }

    public void setDailyPushLimit(int dailyPushLimit) {
        this.dailyPushLimit = dailyPushLimit;
    }
}
