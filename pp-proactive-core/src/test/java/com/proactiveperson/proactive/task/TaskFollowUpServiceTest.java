package com.proactiveperson.proactive.task;

import com.proactiveperson.common.state.InMemoryStateStore;
import com.proactiveperson.memory.stub.InMemoryMemoryService;
import com.proactiveperson.monitor.ActivityStatus;
import com.proactiveperson.proactive.config.DisturbanceProperties;
import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.config.TaskProperties;
import com.proactiveperson.proactive.disturbance.DisturbanceMode;
import com.proactiveperson.proactive.disturbance.DisturbancePreference;
import com.proactiveperson.proactive.disturbance.DisturbancePreferenceService;
import com.proactiveperson.proactive.disturbance.PushPriority;
import com.proactiveperson.proactive.disturbance.cache.InMemoryTtlPreferenceCache;
import com.proactiveperson.proactive.quota.DailyPushQuotaService;
import com.proactiveperson.proactive.user.ProactiveUser;
import com.proactiveperson.proactive.user.ProactiveUserRepository;
import com.proactiveperson.wechat.WeChatMessageGateway;
import com.proactiveperson.wechat.WeChatOutboundChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskFollowUpServiceTest {

    @Mock
    private WeChatMessageGateway weChatMessageGateway;
    @Mock
    private ProactiveUserRepository userRepository;
    @Mock
    private ObjectProvider<com.proactiveperson.agent.Assistant> assistantProvider;

    private InMemoryTaskRepository taskRepository;
    private TaskFollowUpService service;
    private DisturbancePreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        lenient().when(assistantProvider.getIfAvailable()).thenReturn(null);
        taskRepository = new InMemoryTaskRepository();
        preferenceService = new DisturbancePreferenceService(
                new InMemoryMemoryService(), new InMemoryTtlPreferenceCache(), new DisturbanceProperties());
        ProactiveProperties proactiveProperties = new ProactiveProperties();
        proactiveProperties.setDailyPushLimit(2);
        DailyPushQuotaService quotaService = new DailyPushQuotaService(proactiveProperties, new InMemoryStateStore());
        TaskProperties taskProperties = new TaskProperties();
        TaskFollowUpGate gate = new TaskFollowUpGate(
                preferenceService, userId -> ActivityStatus.IDLE, quotaService);
        service = new TaskFollowUpService(
                taskRepository,
                new TaskExtractor(new InMemoryMemoryService(), assistantProvider),
                new SocraticReminderComposer(assistantProvider),
                new TaskPushPriorityResolver(taskProperties),
                gate,
                preferenceService,
                userRepository,
                quotaService,
                weChatMessageGateway,
                new InMemoryMemoryService(),
                taskProperties);

        ProactiveUser user = new ProactiveUser("u1", "oid-1", ZoneId.of("Asia/Shanghai"), true, "上海", false);
        lenient().when(userRepository.findByUserId("u1")).thenReturn(Optional.of(user));
    }

    @Test
    void completeAndSnooze() {
        Task task = service.extractAndSave("u1", "明天记得交工作汇报");
        assertThat(task.status()).isEqualTo(TaskStatus.OPEN);

        Task snoozed = service.snooze(task.id(), Instant.now().plusSeconds(3600));
        assertThat(snoozed.status()).isEqualTo(TaskStatus.SNOOZED);
        assertThat(snoozed.snoozeUntil()).isNotNull();

        Task done = service.complete(task.id());
        assertThat(done.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void nudgeSendsWhenAllowed() {
        Task task = Task.create("u1", "交周报", Instant.now().plusSeconds(2 * 3600), Set.of("工作"));
        taskRepository.save(task);
        when(weChatMessageGateway.sendTextAuto(anyString(), anyString()))
                .thenReturn(WeChatMessageGateway.SendResult.ok(WeChatOutboundChannel.CUSTOMER_SERVICE, "m1"));

        TaskFollowUpService.NudgeOutcome outcome = service.nudge(task.id());

        assertThat(outcome.sent()).isTrue();
        assertThat(outcome.priority()).isEqualTo(PushPriority.HIGH);
        verify(weChatMessageGateway).sendTextAuto(eq("oid-1"), anyString());
    }

    @Test
    void nudgeSkippedWhenQuiet() {
        preferenceService.save("u1", new DisturbancePreference(
                DisturbanceMode.QUIET, null, null, null, Set.of()));
        Task task = Task.create("u1", "交周报", Instant.now().plusSeconds(2 * 3600), Set.of());
        taskRepository.save(task);

        TaskFollowUpService.NudgeOutcome outcome = service.nudge(task.id());

        assertThat(outcome.skipped()).isTrue();
        assertThat(outcome.code()).isEqualTo("QUIET_MODE");
        verify(weChatMessageGateway, never()).sendTextAuto(anyString(), anyString());
    }
}
