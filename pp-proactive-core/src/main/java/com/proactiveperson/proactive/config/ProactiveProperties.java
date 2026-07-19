package com.proactiveperson.proactive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "pp.proactive")
public class ProactiveProperties {

    /** 早间推送调度开关（默认关闭，避免空跑） */
    private boolean morningPushEnabled = false;

    /** 每日主动推送上限 */
    private int dailyPushLimit = 2;

    /** 扫描默认时区（用户未配置时区时使用） */
    private String defaultTimezone = "Asia/Shanghai";

    /** 早间窗口起始小时（含） */
    private int morningWindowStartHour = 8;

    /** 早间窗口结束小时（不含，10 表示 10:00 前） */
    private int morningWindowEndHour = 10;

    /**
     * 候选用户（配置驱动，便于本地验证；生产可换仓储实现）。
     */
    private List<UserEntry> users = new ArrayList<>();

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

    public List<UserEntry> getUsers() {
        return users;
    }

    public void setUsers(List<UserEntry> users) {
        this.users = users == null ? new ArrayList<>() : users;
    }

    public static class UserEntry {
        private String userId;
        private String openId;
        private String timezone;
        private boolean locationAuthorized;
        private String city;
        private boolean doNotDisturb;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public boolean isLocationAuthorized() {
            return locationAuthorized;
        }

        public void setLocationAuthorized(boolean locationAuthorized) {
            this.locationAuthorized = locationAuthorized;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public boolean isDoNotDisturb() {
            return doNotDisturb;
        }

        public void setDoNotDisturb(boolean doNotDisturb) {
            this.doNotDisturb = doNotDisturb;
        }
    }
}
