package com.proactiveperson.proactive.disturbance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.memory.MemoryService;
import com.proactiveperson.proactive.config.DisturbanceProperties;
import com.proactiveperson.proactive.disturbance.cache.PreferenceCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 防干扰偏好：Mem0 长期层持久化 + 缓存加速。
 */
@Service
public class DisturbancePreferenceService {

    public static final String PREF_KEY = "disturbance";
    private static final String CACHE_PREFIX = "pp:disturbance:";
    private static final String MEMORY_PREFIX = "preference:disturbance=";

    private static final Logger log = LoggerFactory.getLogger(DisturbancePreferenceService.class);

    private final MemoryService memoryService;
    private final PreferenceCache preferenceCache;
    private final DisturbanceProperties properties;
    private final ObjectMapper objectMapper;

    public DisturbancePreferenceService(MemoryService memoryService,
                                        PreferenceCache preferenceCache,
                                        DisturbanceProperties properties) {
        this.memoryService = memoryService;
        this.preferenceCache = preferenceCache;
        this.properties = properties;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public DisturbancePreference get(String userId) {
        String cacheKey = CACHE_PREFIX + userId;
        Optional<String> cached = preferenceCache.get(cacheKey);
        if (cached.isPresent()) {
            return deserialize(cached.get()).orElseGet(DisturbancePreference::normal);
        }

        DisturbancePreference loaded = loadFromMemory(userId).orElseGet(DisturbancePreference::normal);
        preferenceCache.put(cacheKey, serialize(loaded), Duration.ofSeconds(properties.getCacheTtlSeconds()));
        return loaded;
    }

    public void save(String userId, DisturbancePreference preference) {
        String json = serialize(preference);
        // updatePreference 会同步写入 LONG_TERM：preference:disturbance=<json>
        memoryService.updatePreference(userId, PREF_KEY, json);
        preferenceCache.put(CACHE_PREFIX + userId, json, Duration.ofSeconds(properties.getCacheTtlSeconds()));
        log.info("disturbance preference saved userId={} mode={}", userId, preference.mode());
    }

    /** 「今天别打扰」：静音至用户本地日末 */
    public DisturbancePreference muteForRestOfDay(String userId, ZoneId zoneId) {
        ZonedDateTime endOfDay = LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId);
        DisturbancePreference current = get(userId);
        DisturbancePreference updated = current.withMutedUntil(endOfDay.toInstant());
        save(userId, updated);
        return updated;
    }

    public DisturbancePreference setMode(String userId, DisturbanceMode mode) {
        DisturbancePreference updated = get(userId).withMode(mode);
        save(userId, updated);
        return updated;
    }

    public DisturbancePreference setCustomHours(String userId, LocalTime start, LocalTime end) {
        DisturbancePreference current = get(userId);
        DisturbancePreference updated = new DisturbancePreference(
                DisturbanceMode.CUSTOM_HOURS,
                start,
                end,
                current.mutedUntil(),
                current.importantTopics());
        save(userId, updated);
        return updated;
    }

    public DisturbancePreference clearMute(String userId) {
        DisturbancePreference current = get(userId);
        DisturbancePreference updated = new DisturbancePreference(
                current.mode(),
                current.customQuietStart(),
                current.customQuietEnd(),
                null,
                current.importantTopics());
        save(userId, updated);
        return updated;
    }

    private Optional<DisturbancePreference> loadFromMemory(String userId) {
        try {
            // updatePreference 写入 preference:disturbance=<json>
            List<String> hits = memoryService.search(userId, MemoryLayer.LONG_TERM, "preference:disturbance", 5);
            for (String hit : hits) {
                if (hit != null && hit.startsWith(MEMORY_PREFIX)) {
                    Optional<DisturbancePreference> parsed = deserialize(hit.substring(MEMORY_PREFIX.length()));
                    if (parsed.isPresent()) {
                        return parsed;
                    }
                }
                Optional<DisturbancePreference> direct = deserialize(hit);
                if (direct.isPresent()) {
                    return direct;
                }
            }
        } catch (Exception ex) {
            log.warn("load disturbance preference failed userId={} cause={}", userId, ex.getMessage());
        }
        return Optional.empty();
    }

    private String serialize(DisturbancePreference preference) {
        try {
            return objectMapper.writeValueAsString(new Wire(
                    preference.mode().name(),
                    preference.customQuietStart() == null ? null : preference.customQuietStart().toString(),
                    preference.customQuietEnd() == null ? null : preference.customQuietEnd().toString(),
                    preference.mutedUntil() == null ? null : preference.mutedUntil().toString(),
                    preference.importantTopics()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize disturbance preference failed", e);
        }
    }

    private Optional<DisturbancePreference> deserialize(String json) {
        if (!StringUtils.hasText(json) || !json.trim().startsWith("{")) {
            return Optional.empty();
        }
        try {
            Wire wire = objectMapper.readValue(json, Wire.class);
            DisturbanceMode mode = DisturbanceMode.valueOf(wire.mode());
            LocalTime start = wire.quietStart() == null ? null : LocalTime.parse(wire.quietStart());
            LocalTime end = wire.quietEnd() == null ? null : LocalTime.parse(wire.quietEnd());
            Instant muted = wire.mutedUntil() == null ? null : Instant.parse(wire.mutedUntil());
            return Optional.of(new DisturbancePreference(mode, start, end, muted, wire.importantTopics()));
        } catch (Exception ex) {
            log.warn("deserialize disturbance preference failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private record Wire(
            String mode,
            String quietStart,
            String quietEnd,
            String mutedUntil,
            Set<String> importantTopics
    ) {
    }
}
