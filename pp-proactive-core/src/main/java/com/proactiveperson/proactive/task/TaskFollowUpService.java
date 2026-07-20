package com.proactiveperson.proactive.task;

import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.memory.MemoryService;
import com.proactiveperson.proactive.config.TaskProperties;
import com.proactiveperson.proactive.disturbance.DisturbancePreference;
import com.proactiveperson.proactive.disturbance.DisturbancePreferenceService;
import com.proactiveperson.proactive.disturbance.PushPriority;
import com.proactiveperson.proactive.quota.DailyPushQuotaService;
import com.proactiveperson.proactive.user.ProactiveUser;
import com.proactiveperson.proactive.user.ProactiveUserRepository;
import com.proactiveperson.wechat.WeChatMessageGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class TaskFollowUpService {

    private static final Logger log = LoggerFactory.getLogger(TaskFollowUpService.class);

    private final TaskRepository taskRepository;
    private final TaskExtractor taskExtractor;
    private final SocraticReminderComposer reminderComposer;
    private final TaskPushPriorityResolver priorityResolver;
    private final TaskFollowUpGate followUpGate;
    private final DisturbancePreferenceService preferenceService;
    private final ProactiveUserRepository userRepository;
    private final DailyPushQuotaService quotaService;
    private final WeChatMessageGateway weChatMessageGateway;
    private final MemoryService memoryService;
    private final TaskProperties taskProperties;

    public TaskFollowUpService(TaskRepository taskRepository,
                               TaskExtractor taskExtractor,
                               SocraticReminderComposer reminderComposer,
                               TaskPushPriorityResolver priorityResolver,
                               TaskFollowUpGate followUpGate,
                               DisturbancePreferenceService preferenceService,
                               ProactiveUserRepository userRepository,
                               DailyPushQuotaService quotaService,
                               WeChatMessageGateway weChatMessageGateway,
                               MemoryService memoryService,
                               TaskProperties taskProperties) {
        this.taskRepository = taskRepository;
        this.taskExtractor = taskExtractor;
        this.reminderComposer = reminderComposer;
        this.priorityResolver = priorityResolver;
        this.followUpGate = followUpGate;
        this.preferenceService = preferenceService;
        this.userRepository = userRepository;
        this.quotaService = quotaService;
        this.weChatMessageGateway = weChatMessageGateway;
        this.memoryService = memoryService;
        this.taskProperties = taskProperties;
    }

    public Task extractAndSave(String userId, String text) {
        ProactiveUser user = resolveUser(userId);
        Task task = taskExtractor.extract(user.userId(), text, user.timezone());
        Task saved = taskRepository.save(task);
        try {
            memoryService.add(user.userId(), MemoryLayer.MID_TERM,
                    "task:" + saved.id() + " title=" + saved.title());
        } catch (RuntimeException ex) {
            log.warn("task mid-term persist failed userId={} cause={}", user.userId(), ex.getMessage());
        }
        return saved;
    }

    public List<Task> list(String userId, TaskStatus status) {
        if (status == null) {
            return taskRepository.findByUserId(userId);
        }
        return taskRepository.findByUserIdAndStatus(userId, status);
    }

    public Task complete(String taskId) {
        Task task = requireTask(taskId);
        Task updated = task.withStatus(TaskStatus.DONE, null);
        return taskRepository.save(updated);
    }

    public Task snooze(String taskId, Instant until) {
        Task task = requireTask(taskId);
        Instant snoozeUntil = until;
        if (snoozeUntil == null) {
            snoozeUntil = Instant.now().plusSeconds(Math.max(1, taskProperties.getDefaultSnoozeHours()) * 3600L);
        }
        Task updated = task.withStatus(TaskStatus.SNOOZED, snoozeUntil);
        return taskRepository.save(updated);
    }

    public NudgeOutcome nudge(String taskId) {
        Task task = requireTask(taskId);
        Instant now = Instant.now();
        if (!task.isNudgeEligible(now)) {
            return NudgeOutcome.skipped("NOT_ELIGIBLE", "任务已完成或仍在暂缓期");
        }

        ProactiveUser user = resolveUser(task.userId());
        DisturbancePreference preference = preferenceService.get(user.userId());
        PushPriority priority = priorityResolver.resolve(task, preference.importantTopics(), now);
        TaskFollowUpGate.Decision decision = followUpGate.evaluate(user, priority);
        if (!decision.allowed()) {
            return NudgeOutcome.skipped(decision.code(), decision.reason());
        }

        DailyPushQuotaService.ReserveResult reserved =
                quotaService.tryReserveDaily(user, decision.nowInUserZone());
        if (reserved != DailyPushQuotaService.ReserveResult.RESERVED) {
            return NudgeOutcome.skipped(reserved.name(), "日配额预占失败");
        }

        String message = reminderComposer.compose(task, user.timezone());
        WeChatMessageGateway.SendResult send = weChatMessageGateway.sendTextAuto(user.openId(), message);
        if (!send.success()) {
            quotaService.releaseDaily(user, decision.nowInUserZone());
            log.warn("task nudge send failed taskId={} detail={}", taskId, send.detail());
            return NudgeOutcome.failed(send.detail(), message, priority);
        }

        // 若此前为 SNOOZED 且已到期，nudge 后回到 OPEN
        if (task.status() == TaskStatus.SNOOZED) {
            taskRepository.save(task.withStatus(TaskStatus.OPEN, null));
        }
        return NudgeOutcome.sent(send.messageId(), send.channel().name(), message, priority);
    }

    private Task requireTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));
    }

    private ProactiveUser resolveUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        Optional<ProactiveUser> found = userRepository.findByUserId(userId);
        if (found.isPresent()) {
            return found.get();
        }
        // 未配置候选用户时仍允许抽取/状态变更；推送需 openId
        return new ProactiveUser(userId, "unknown-openid", ZoneId.of("Asia/Shanghai"), false, null, false);
    }

    public record NudgeOutcome(boolean sent, boolean skipped, String code, String detail,
                               String message, PushPriority priority) {

        static NudgeOutcome skipped(String code, String detail) {
            return new NudgeOutcome(false, true, code, detail, null, null);
        }

        static NudgeOutcome failed(String detail, String message, PushPriority priority) {
            return new NudgeOutcome(false, false, "SEND_FAILED", detail, message, priority);
        }

        static NudgeOutcome sent(String messageId, String channel, String message, PushPriority priority) {
            return new NudgeOutcome(true, false, "SENT", channel + ":" + messageId, message, priority);
        }
    }
}
