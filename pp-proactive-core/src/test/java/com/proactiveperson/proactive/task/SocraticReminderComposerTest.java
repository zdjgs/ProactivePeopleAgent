package com.proactiveperson.proactive.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SocraticReminderComposerTest {

    @Test
    void templateIsQuestionAndAvoidsPushyWords() {
        @SuppressWarnings("unchecked")
        ObjectProvider<com.proactiveperson.agent.Assistant> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        SocraticReminderComposer composer = new SocraticReminderComposer(provider);

        Task task = Task.create("u1", "写周报", Instant.parse("2026-07-20T12:00:00Z"), Set.of());
        String message = composer.compose(task, ZoneId.of("Asia/Shanghai"));

        assertThat(message).contains("？");
        assertThat(SocraticReminderComposer.containsForbidden(message)).isFalse();
        assertThat(message).doesNotContain("赶紧", "必须完成");
    }
}
