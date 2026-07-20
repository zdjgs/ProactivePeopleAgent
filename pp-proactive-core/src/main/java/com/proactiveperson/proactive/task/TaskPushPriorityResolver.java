package com.proactiveperson.proactive.task;

import com.proactiveperson.proactive.config.TaskProperties;
import com.proactiveperson.proactive.disturbance.PushPriority;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/**
 * 承接 REQ-006：判定任务跟进推送优先级。
 */
@Component
public class TaskPushPriorityResolver {

    private final TaskProperties properties;

    public TaskPushPriorityResolver(TaskProperties properties) {
        this.properties = properties;
    }

    public PushPriority resolve(Task task, Set<String> importantTopics, Instant now) {
        if (task == null) {
            return PushPriority.NORMAL;
        }
        if (dueWithinHighWindow(task.dueAt(), now)) {
            return PushPriority.HIGH;
        }
        if (topicHit(task, importantTopics)) {
            return PushPriority.HIGH;
        }
        return PushPriority.NORMAL;
    }

    boolean dueWithinHighWindow(Instant dueAt, Instant now) {
        if (dueAt == null || now == null) {
            return false;
        }
        long deltaSec = dueAt.getEpochSecond() - now.getEpochSecond();
        long window = Math.max(1, properties.getDueHighHours()) * 3600L;
        return deltaSec >= 0 && deltaSec <= window;
    }

    static boolean topicHit(Task task, Set<String> importantTopics) {
        if (importantTopics == null || importantTopics.isEmpty()) {
            return false;
        }
        String title = task.title() == null ? "" : task.title().toLowerCase(Locale.ROOT);
        for (String topic : importantTopics) {
            if (topic == null || topic.isBlank()) {
                continue;
            }
            String t = topic.toLowerCase(Locale.ROOT);
            if (title.contains(t)) {
                return true;
            }
            for (String taskTopic : task.topics()) {
                if (taskTopic != null && taskTopic.toLowerCase(Locale.ROOT).contains(t)) {
                    return true;
                }
            }
        }
        return false;
    }
}
