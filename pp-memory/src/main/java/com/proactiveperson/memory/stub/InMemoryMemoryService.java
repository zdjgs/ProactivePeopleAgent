package com.proactiveperson.memory.stub;

import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.memory.MemoryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "pp.memory.provider", havingValue = "stub", matchIfMissing = true)
public class InMemoryMemoryService implements MemoryService {

    private final Map<String, List<String>> store = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> preferences = new ConcurrentHashMap<>();

    @Override
    public void add(String userId, MemoryLayer layer, String content) {
        store.computeIfAbsent(key(userId, layer), k -> new ArrayList<>()).add(content);
    }

    @Override
    public List<String> search(String userId, MemoryLayer layer, String query, int limit) {
        List<String> items = store.getOrDefault(key(userId, layer), List.of());
        if (query == null || query.isBlank()) {
            return items.stream().limit(limit).toList();
        }
        String q = query.toLowerCase();
        return items.stream()
                .filter(item -> item.toLowerCase().contains(q))
                .limit(limit)
                .toList();
    }

    @Override
    public void updatePreference(String userId, String key, String value) {
        preferences.computeIfAbsent(userId, id -> new ConcurrentHashMap<>()).put(key, value);
        add(userId, MemoryLayer.LONG_TERM, "preference:" + key + "=" + value);
    }

    private static String key(String userId, MemoryLayer layer) {
        return userId + ":" + layer.name();
    }
}
