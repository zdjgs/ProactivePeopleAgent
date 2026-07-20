package com.proactiveperson.proactive.web;

import com.proactiveperson.proactive.task.Task;
import com.proactiveperson.proactive.task.TaskFollowUpService;
import com.proactiveperson.proactive.task.TaskStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskFollowUpService taskFollowUpService;

    public TaskController(TaskFollowUpService taskFollowUpService) {
        this.taskFollowUpService = taskFollowUpService;
    }

    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extract(@RequestBody ExtractRequest request) {
        if (request == null || !StringUtils.hasText(request.userId())) {
            return ResponseEntity.badRequest().build();
        }
        Task task = taskFollowUpService.extractAndSave(request.userId(), request.text());
        return ResponseEntity.ok(toView(task));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@RequestParam("userId") String userId,
                                                          @RequestParam(value = "status", required = false) TaskStatus status) {
        if (!StringUtils.hasText(userId)) {
            return ResponseEntity.badRequest().build();
        }
        List<Map<String, Object>> body = taskFollowUpService.list(userId, status).stream()
                .map(TaskController::toView)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> complete(@PathVariable("id") String id) {
        return ResponseEntity.ok(toView(taskFollowUpService.complete(id)));
    }

    @PostMapping("/{id}/snooze")
    public ResponseEntity<Map<String, Object>> snooze(@PathVariable("id") String id,
                                                      @RequestBody(required = false) SnoozeRequest request) {
        Instant until = null;
        if (request != null) {
            until = request.until();
            if (until == null && request.hours() != null && request.hours() > 0) {
                until = Instant.now().plusSeconds(request.hours() * 3600L);
            }
        }
        return ResponseEntity.ok(toView(taskFollowUpService.snooze(id, until)));
    }

    @PostMapping("/{id}/nudge")
    public ResponseEntity<Map<String, Object>> nudge(@PathVariable("id") String id) {
        TaskFollowUpService.NudgeOutcome outcome = taskFollowUpService.nudge(id);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sent", outcome.sent());
        body.put("skipped", outcome.skipped());
        body.put("code", outcome.code());
        body.put("detail", outcome.detail());
        body.put("message", outcome.message());
        body.put("priority", outcome.priority() == null ? null : outcome.priority().name());
        return ResponseEntity.ok(body);
    }

    private static Map<String, Object> toView(Task task) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", task.id());
        view.put("userId", task.userId());
        view.put("title", task.title());
        view.put("dueAt", task.dueAt() == null ? null : task.dueAt().toString());
        view.put("topics", task.topicList());
        view.put("status", task.status().name());
        view.put("snoozeUntil", task.snoozeUntil() == null ? null : task.snoozeUntil().toString());
        view.put("createdAt", task.createdAt().toString());
        view.put("updatedAt", task.updatedAt().toString());
        return view;
    }

    public record ExtractRequest(String userId, String text) {
    }

    public record SnoozeRequest(Instant until, Integer hours) {
    }
}
