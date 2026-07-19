package com.proactiveperson.common.state;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryStateStoreTest {

    @Test
    void setIfAbsentAndIncrementAreAtomicStyle() {
        InMemoryStateStore store = new InMemoryStateStore();

        assertThat(store.setIfAbsent("m", "1", Duration.ofHours(1))).isTrue();
        assertThat(store.setIfAbsent("m", "1", Duration.ofHours(1))).isFalse();
        assertThat(store.increment("q", Duration.ofHours(1))).isEqualTo(1);
        assertThat(store.increment("q", Duration.ofHours(1))).isEqualTo(2);
        assertThat(store.decrement("q")).isEqualTo(1);
        store.delete("m");
        assertThat(store.setIfAbsent("m", "1", Duration.ofHours(1))).isTrue();
    }
}
