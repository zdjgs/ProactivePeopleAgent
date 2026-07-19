package com.proactiveperson.proactive.user;

import java.time.ZoneId;
import java.util.Objects;

/**
 * 早间主动推送候选用户。
 */
public final class ProactiveUser {

    private final String userId;
    private final String openId;
    private final ZoneId timezone;
    private final boolean locationAuthorized;
    private final String city;
    private final boolean doNotDisturb;

    public ProactiveUser(String userId,
                         String openId,
                         ZoneId timezone,
                         boolean locationAuthorized,
                         String city,
                         boolean doNotDisturb) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.openId = Objects.requireNonNull(openId, "openId");
        this.timezone = timezone == null ? ZoneId.of("Asia/Shanghai") : timezone;
        this.locationAuthorized = locationAuthorized;
        this.city = city;
        this.doNotDisturb = doNotDisturb;
    }

    public String userId() {
        return userId;
    }

    public String openId() {
        return openId;
    }

    public ZoneId timezone() {
        return timezone;
    }

    public boolean locationAuthorized() {
        return locationAuthorized;
    }

    public String city() {
        return city;
    }

    public boolean doNotDisturb() {
        return doNotDisturb;
    }
}
