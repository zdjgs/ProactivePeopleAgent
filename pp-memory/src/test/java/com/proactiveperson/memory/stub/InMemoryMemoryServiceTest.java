package com.proactiveperson.memory.stub;

import com.proactiveperson.common.domain.MemoryLayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMemoryServiceTest {

    private final InMemoryMemoryService service = new InMemoryMemoryService();

    @Test
    void layersAreIsolated() {
        service.add("u1", MemoryLayer.SHORT_TERM, "short");
        service.add("u1", MemoryLayer.LONG_TERM, "long");

        assertThat(service.search("u1", MemoryLayer.SHORT_TERM, null, 10)).containsExactly("short");
        assertThat(service.search("u1", MemoryLayer.LONG_TERM, null, 10)).containsExactly("long");
    }

    @Test
    void updatePreferenceIsReadableViaLongTermSearch() {
        service.updatePreference("u1", "timezone", "Asia/Shanghai");

        List<String> prefs = service.search("u1", MemoryLayer.LONG_TERM, "timezone", 5);
        assertThat(prefs).isNotEmpty();
    }
}
