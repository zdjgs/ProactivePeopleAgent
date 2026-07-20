package com.proactiveperson.proactive.task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findById(String id);

    List<Task> findByUserId(String userId);

    List<Task> findByUserIdAndStatus(String userId, TaskStatus status);
}
