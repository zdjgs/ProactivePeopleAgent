package com.proactiveperson.proactive.task;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTaskRepository implements TaskRepository {

    private final Map<String, Task> store = new ConcurrentHashMap<>();

    @Override
    public Task save(Task task) {
        store.put(task.id(), task);
        return task;
    }

    @Override
    public Optional<Task> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Task> findByUserId(String userId) {
        List<Task> result = new ArrayList<>();
        for (Task task : store.values()) {
            if (task.userId().equals(userId)) {
                result.add(task);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public List<Task> findByUserIdAndStatus(String userId, TaskStatus status) {
        return findByUserId(userId).stream()
                .filter(t -> t.status() == status)
                .toList();
    }

    void clear() {
        store.clear();
    }
}
