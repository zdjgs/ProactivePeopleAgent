package com.proactiveperson.proactive.disturbance;

import com.proactiveperson.memory.stub.InMemoryMemoryService;
import com.proactiveperson.proactive.config.DisturbanceProperties;
import com.proactiveperson.proactive.disturbance.cache.InMemoryTtlPreferenceCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DisturbancePreferenceServiceTest {

    private DisturbancePreferenceService service;
    private InMemoryTtlPreferenceCache cache;

    @BeforeEach
    void setUp() {
        cache = new InMemoryTtlPreferenceCache();
        DisturbanceProperties properties = new DisturbanceProperties();
        properties.setCacheTtlSeconds(60);
        service = new DisturbancePreferenceService(new InMemoryMemoryService(), cache, properties);
    }

    @Test
    void persistsAndLoadsFromMemory() {
        service.setMode("u1", DisturbanceMode.IMPORTANT_ONLY);

        DisturbancePreference loaded = service.get("u1");

        assertThat(loaded.mode()).isEqualTo(DisturbanceMode.IMPORTANT_ONLY);
    }

    @Test
    void usesCacheOnSecondRead() {
        service.setMode("u1", DisturbanceMode.QUIET);
        assertThat(cache.get("pp:disturbance:u1")).isPresent();

        // 清掉底层记忆不可直接做；改为改缓存后验证命中缓存
        cache.put("pp:disturbance:u1",
                "{\"mode\":\"IMPORTANT_ONLY\",\"quietStart\":null,\"quietEnd\":null,\"mutedUntil\":null,\"importantTopics\":[]}",
                java.time.Duration.ofMinutes(1));

        assertThat(service.get("u1").mode()).isEqualTo(DisturbanceMode.IMPORTANT_ONLY);
    }

    @Test
    void muteForRestOfDaySetsMutedUntil() {
        DisturbancePreference muted = service.muteForRestOfDay("u1", ZoneId.of("Asia/Shanghai"));
        assertThat(muted.mutedUntil()).isNotNull();
        assertThat(service.get("u1").mutedUntil()).isEqualTo(muted.mutedUntil());
    }
}
