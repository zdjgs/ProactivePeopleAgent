package com.proactiveperson.proactive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pp.task")
public class TaskProperties {

    /** 调度扫描开关（本切片默认关闭，nudge 走 API） */
    private boolean followUpEnabled = false;

    /** 截止前多少小时视为高优先级 */
    private int dueHighHours = 24;

    /** 暂缓默认小时数 */
    private int defaultSnoozeHours = 4;

    public boolean isFollowUpEnabled() {
        return followUpEnabled;
    }

    public void setFollowUpEnabled(boolean followUpEnabled) {
        this.followUpEnabled = followUpEnabled;
    }

    public int getDueHighHours() {
        return dueHighHours;
    }

    public void setDueHighHours(int dueHighHours) {
        this.dueHighHours = dueHighHours;
    }

    public int getDefaultSnoozeHours() {
        return defaultSnoozeHours;
    }

    public void setDefaultSnoozeHours(int defaultSnoozeHours) {
        this.defaultSnoozeHours = defaultSnoozeHours;
    }
}
