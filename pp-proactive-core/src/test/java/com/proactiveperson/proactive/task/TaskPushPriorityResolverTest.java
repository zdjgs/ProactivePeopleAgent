package com.proactiveperson.proactive.task;

import com.proactiveperson.proactive.config.TaskProperties;
import com.proactiveperson.proactive.disturbance.PushPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TaskPushPriorityResolverTest {

    private TaskPushPriorityResolver resolver;

    @BeforeEach
    void setUp() {
        TaskProperties properties = new TaskProperties();
        properties.setDueHighHours(24);
        resolver = new TaskPushPriorityResolver(properties);
    }

    @Test
    void highWhenDueWithin24h() {
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        Task task = Task.create("u1", "交周报", now.plusSeconds(12 * 3600), Set.of());

        assertThat(resolver.resolve(task, Set.of(), now)).isEqualTo(PushPriority.HIGH);
    }

    @Test
    void highWhenImportantTopicHit() {
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        Task task = Task.create("u1", "准备季度汇报", now.plusSeconds(72 * 3600), Set.of("工作"));

        assertThat(resolver.resolve(task, Set.of("汇报"), now)).isEqualTo(PushPriority.HIGH);
    }

    @Test
    void normalOtherwise() {
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        Task task = Task.create("u1", "买菜", now.plusSeconds(72 * 3600), Set.of("生活"));

        assertThat(resolver.resolve(task, Set.of("工作"), now)).isEqualTo(PushPriority.NORMAL);
    }
}
