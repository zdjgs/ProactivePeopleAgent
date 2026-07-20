package com.proactiveperson.proactive.task;

import com.proactiveperson.memory.stub.InMemoryMemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskExtractorTest {

    private TaskExtractor extractor;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ObjectProvider<com.proactiveperson.agent.Assistant> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        extractor = new TaskExtractor(new InMemoryMemoryService(), provider);
    }

    @Test
    void heuristicExtractsTitleAndDue() {
        Task task = extractor.extract("u1", "明天记得交工作汇报", ZoneId.of("Asia/Shanghai"));

        assertThat(task.title()).contains("交工作汇报");
        assertThat(task.dueAt()).isNotNull();
        assertThat(task.topics()).contains("工作");
    }

    @Test
    void parseJsonSchemaShape() {
        TaskExtractor.TaskDraft draft = extractor.parseJson("""
                {"title":"买菜","dueAt":"2026-07-21T12:00:00Z","topics":["生活"]}
                """);

        assertThat(draft).isNotNull();
        assertThat(draft.title()).isEqualTo("买菜");
        assertThat(draft.dueAt()).isEqualTo(java.time.Instant.parse("2026-07-21T12:00:00Z"));
        assertThat(draft.topics()).contains("生活");
    }
}
