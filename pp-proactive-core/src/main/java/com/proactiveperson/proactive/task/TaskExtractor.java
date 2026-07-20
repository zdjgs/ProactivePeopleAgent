package com.proactiveperson.proactive.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proactiveperson.agent.Assistant;
import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.memory.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从对话文本抽取结构化 Task（LLM JSON 优先，失败则确定性规则 fallback）。
 */
@Component
public class TaskExtractor {

    private static final Logger log = LoggerFactory.getLogger(TaskExtractor.class);

    /**
     * JsonSchema 形态说明（注入 Prompt，约束 LLM 输出）。
     */
    public static final String TASK_JSON_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["title"],
              "properties": {
                "title": { "type": "string", "minLength": 1 },
                "dueAt": { "type": ["string", "null"], "description": "ISO-8601 instant or date" },
                "topics": { "type": "array", "items": { "type": "string" } }
              },
              "additionalProperties": false
            }
            """;

    private static final Pattern DUE_TOMORROW = Pattern.compile("明天|明日");
    private static final Pattern DUE_TODAY = Pattern.compile("今天|今日|今晚");
    private static final Pattern DUE_HOURS = Pattern.compile("(\\d+)\\s*小时内");
    private static final Pattern TITLE_HINT = Pattern.compile(
            "(?:记得|别忘了|要|需要|帮我|提醒我)?\\s*(.+?)(?:，|。|！|？|$)");

    private final MemoryService memoryService;
    private final ObjectProvider<Assistant> assistantProvider;
    private final ObjectMapper objectMapper;

    public TaskExtractor(MemoryService memoryService, ObjectProvider<Assistant> assistantProvider) {
        this.memoryService = memoryService;
        this.assistantProvider = assistantProvider;
        this.objectMapper = new ObjectMapper();
    }

    public Task extract(String userId, String text, ZoneId zoneId) {
        String corpus = resolveCorpus(userId, text);
        if (!StringUtils.hasText(corpus)) {
            throw new IllegalArgumentException("无可抽取的文本（请传入 text 或先有短期对话记忆）");
        }

        Assistant assistant = assistantProvider.getIfAvailable();
        if (assistant != null) {
            try {
                String json = assistant.complete(buildPrompt(corpus)).trim();
                TaskDraft draft = parseJson(json);
                if (draft != null && StringUtils.hasText(draft.title())) {
                    return Task.create(userId, draft.title().trim(), draft.dueAt(), draft.topics());
                }
            } catch (RuntimeException ex) {
                log.warn("task llm extract failed, fallback heuristic: {}", ex.getMessage());
            }
        }
        return heuristicExtract(userId, corpus, zoneId == null ? ZoneId.of("Asia/Shanghai") : zoneId);
    }

    private String resolveCorpus(String userId, String text) {
        if (StringUtils.hasText(text)) {
            return text.trim();
        }
        List<String> hits = memoryService.search(userId, MemoryLayer.SHORT_TERM, "", 8);
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        return String.join("\n", hits);
    }

    private static String buildPrompt(String corpus) {
        return """
                从下面用户对话中抽取一个待办任务，严格输出单个 JSON 对象（不要 markdown）。
                JsonSchema:
                %s
                对话:
                %s
                """.formatted(TASK_JSON_SCHEMA, corpus);
    }

    TaskDraft parseJson(String raw) {
        String json = stripFence(raw);
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.hasNonNull("title")) {
                return null;
            }
            String title = node.get("title").asText("").trim();
            Instant dueAt = null;
            if (node.has("dueAt") && !node.get("dueAt").isNull()) {
                dueAt = parseDue(node.get("dueAt").asText());
            }
            Set<String> topics = new HashSet<>();
            if (node.has("topics") && node.get("topics").isArray()) {
                for (JsonNode t : node.get("topics")) {
                    if (t != null && StringUtils.hasText(t.asText())) {
                        topics.add(t.asText().trim());
                    }
                }
            }
            return new TaskDraft(title, dueAt, topics);
        } catch (Exception ex) {
            log.debug("parse task json failed: {}", ex.getMessage());
            return null;
        }
    }

    private static Instant parseDue(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDate.parse(raw.trim()).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    Task heuristicExtract(String userId, String corpus, ZoneId zoneId) {
        String line = corpus.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .reduce((a, b) -> b)
                .orElse(corpus.trim());
        String cleaned = line.replaceFirst("(?i)^user:\\s*", "")
                .replaceFirst("(?i)^assistant:\\s*", "")
                .trim();

        Instant dueAt = null;
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (DUE_TODAY.matcher(cleaned).find()) {
            dueAt = LocalDate.now(zoneId).atTime(20, 0).atZone(zoneId).toInstant();
        } else if (DUE_TOMORROW.matcher(cleaned).find()) {
            dueAt = LocalDate.now(zoneId).plusDays(1).atTime(20, 0).atZone(zoneId).toInstant();
        } else {
            Matcher hours = DUE_HOURS.matcher(cleaned);
            if (hours.find()) {
                dueAt = Instant.now().plusSeconds(Long.parseLong(hours.group(1)) * 3600L);
            }
        }

        String title = cleaned;
        Matcher hint = TITLE_HINT.matcher(cleaned);
        if (hint.find() && StringUtils.hasText(hint.group(1))) {
            title = hint.group(1).trim();
        }
        if (title.length() > 80) {
            title = title.substring(0, 80);
        }

        Set<String> topics = inferTopics(cleaned);
        return Task.create(userId, title, dueAt, topics);
    }

    private static Set<String> inferTopics(String text) {
        List<String> keywords = List.of("工作", "健康", "运动", "学习", "家庭", "财务", "deadline", "汇报");
        Set<String> topics = new HashSet<>();
        String lower = text.toLowerCase(Locale.ROOT);
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase(Locale.ROOT))) {
                topics.add(kw);
            }
        }
        return topics;
    }

    private static String stripFence(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                return s.substring(firstNl + 1, lastFence).trim();
            }
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
    }

    record TaskDraft(String title, Instant dueAt, Set<String> topics) {
    }
}
