package com.proactiveperson.proactive.disturbance;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Set;

/**
 * 用户防干扰偏好（持久化到 Mem0 长期层，经缓存加速读取）。
 */
public final class DisturbancePreference {

    private final DisturbanceMode mode;
    private final LocalTime customQuietStart;
    private final LocalTime customQuietEnd;
    /** 「今天别打扰」临时静音截止时间（用户本地日末或显式截止） */
    private final Instant mutedUntil;
    private final Set<String> importantTopics;

    public DisturbancePreference(DisturbanceMode mode,
                                 LocalTime customQuietStart,
                                 LocalTime customQuietEnd,
                                 Instant mutedUntil,
                                 Set<String> importantTopics) {
        this.mode = mode == null ? DisturbanceMode.NORMAL : mode;
        this.customQuietStart = customQuietStart;
        this.customQuietEnd = customQuietEnd;
        this.mutedUntil = mutedUntil;
        this.importantTopics = importantTopics == null ? Set.of() : Set.copyOf(importantTopics);
    }

    public static DisturbancePreference normal() {
        return new DisturbancePreference(DisturbanceMode.NORMAL, null, null, null, Set.of());
    }

    public static DisturbancePreference quiet() {
        return new DisturbancePreference(DisturbanceMode.QUIET, null, null, null, Set.of());
    }

    public DisturbanceMode mode() {
        return mode;
    }

    public LocalTime customQuietStart() {
        return customQuietStart;
    }

    public LocalTime customQuietEnd() {
        return customQuietEnd;
    }

    public Instant mutedUntil() {
        return mutedUntil;
    }

    public Set<String> importantTopics() {
        return importantTopics;
    }

    public DisturbancePreference withMutedUntil(Instant until) {
        return new DisturbancePreference(mode, customQuietStart, customQuietEnd, until, importantTopics);
    }

    public DisturbancePreference withMode(DisturbanceMode newMode) {
        return new DisturbancePreference(newMode, customQuietStart, customQuietEnd, mutedUntil, importantTopics);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DisturbancePreference that)) {
            return false;
        }
        return mode == that.mode
                && Objects.equals(customQuietStart, that.customQuietStart)
                && Objects.equals(customQuietEnd, that.customQuietEnd)
                && Objects.equals(mutedUntil, that.mutedUntil)
                && Objects.equals(importantTopics, that.importantTopics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, customQuietStart, customQuietEnd, mutedUntil, importantTopics);
    }
}
