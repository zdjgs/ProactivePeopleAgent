package com.proactiveperson.proactive.user;

import com.proactiveperson.proactive.config.ProactiveProperties;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ConfigProactiveUserRepository implements ProactiveUserRepository {

    private final ProactiveProperties properties;

    public ConfigProactiveUserRepository(ProactiveProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<ProactiveUser> findAllCandidates() {
        List<ProactiveUser> users = new ArrayList<>();
        for (ProactiveProperties.UserEntry entry : properties.getUsers()) {
            if (!StringUtils.hasText(entry.getUserId()) || !StringUtils.hasText(entry.getOpenId())) {
                continue;
            }
            ZoneId zone = resolveZone(entry.getTimezone());
            users.add(new ProactiveUser(
                    entry.getUserId(),
                    entry.getOpenId(),
                    zone,
                    entry.isLocationAuthorized(),
                    entry.getCity(),
                    entry.isDoNotDisturb()));
        }
        return List.copyOf(users);
    }

    private ZoneId resolveZone(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            return ZoneId.of(properties.getDefaultTimezone());
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception ex) {
            return ZoneId.of(properties.getDefaultTimezone());
        }
    }
}
