package com.proactiveperson.proactive.task;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 结构化任务（JsonSchema 抽取结果持久化形态）。
 */
public final class Task {

    private final String id;
    private final String userId;
    private final String title;
    private final Instant dueAt;
    private final Set<String> topics;
    private final TaskStatus status;
    private final Instant snoozeUntil;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Task(String id,
                String userId,
                String title,
                Instant dueAt,
                Set<String> topics,
                TaskStatus status,
                Instant snoozeUntil,
                Instant createdAt,
                Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.title = Objects.requireNonNull(title, "title");
        this.dueAt = dueAt;
        this.topics = topics == null ? Set.of() : Set.copyOf(topics);
        this.status = status == null ? TaskStatus.OPEN : status;
        this.snoozeUntil = snoozeUntil;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    public static Task create(String userId, String title, Instant dueAt, Set<String> topics) {
        Instant now = Instant.now();
        return new Task(UUID.randomUUID().toString(), userId, title, dueAt, topics, TaskStatus.OPEN, null, now, now);
    }

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String title() {
        return title;
    }

    public Instant dueAt() {
        return dueAt;
    }

    public Set<String> topics() {
        return topics;
    }

    public TaskStatus status() {
        return status;
    }

    public Instant snoozeUntil() {
        return snoozeUntil;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Task withStatus(TaskStatus newStatus, Instant snoozeUntil) {
        return new Task(id, userId, title, dueAt, topics, newStatus, snoozeUntil, createdAt, Instant.now());
    }

    public boolean isNudgeEligible(Instant now) {
        if (status == TaskStatus.DONE) {
            return false;
        }
        if (status == TaskStatus.SNOOZED && snoozeUntil != null && now.isBefore(snoozeUntil)) {
            return false;
        }
        return true;
    }

    public List<String> topicList() {
        return List.copyOf(topics);
    }
}
