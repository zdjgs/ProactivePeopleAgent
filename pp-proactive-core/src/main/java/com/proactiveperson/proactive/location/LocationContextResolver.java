package com.proactiveperson.proactive.location;

import com.proactiveperson.proactive.user.ProactiveUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 定位上下文：须用户显式授权；未授权则降级为通用语境（设计方案红线）。
 */
@Service
public class LocationContextResolver {

    public LocationContext resolve(ProactiveUser user) {
        if (!user.locationAuthorized()) {
            return LocationContext.degraded("用户未授权定位，使用通用语境");
        }
        if (!StringUtils.hasText(user.city())) {
            return LocationContext.degraded("已授权但缺少城市信息，使用通用语境");
        }
        return LocationContext.authorized(user.city());
    }

    public record LocationContext(boolean authorized, String cityOrLabel, String note) {

        public static LocationContext authorized(String city) {
            return new LocationContext(true, city, "ok");
        }

        public static LocationContext degraded(String note) {
            return new LocationContext(false, "通用", note);
        }
    }
}
