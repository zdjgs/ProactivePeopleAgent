package com.proactiveperson.proactive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pp.proactive")
public class ProactiveProperties {

    /** 早间推送调度开关（默认关闭，避免空跑） */
    private boolean morningPushEnabled = false;

    /** 每日主动推送上限 */
    private int dailyPushLimit = 2;

    /** 扫描默认时区（后续改为按用户偏好） */
    private String defaultTimezone = "Asia/Shanghai";

    /** 早间窗口起始小时（含） */
    private int morningWindowStartHour = 8;

    /** 早间窗口结束小时（不含，10 表示 10:00 前） */
    private int morningWindowEndHour = 10;

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

    public String getDefaultTimezone() {
        return defaultTimezone;
    }

    public void setDefaultTimezone(String defaultTimezone) {
        this.defaultTimezone = defaultTimezone;
    }

    public int getMorningWindowStartHour() {
        return morningWindowStartHour;
    }

    public void setMorningWindowStartHour(int morningWindowStartHour) {
        this.morningWindowStartHour = morningWindowStartHour;
    }

    public int getMorningWindowEndHour() {
        return morningWindowEndHour;
    }

    public void setMorningWindowEndHour(int morningWindowEndHour) {
        this.morningWindowEndHour = morningWindowEndHour;
    }
}
