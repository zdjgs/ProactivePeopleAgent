package com.proactiveperson.memory;

import com.proactiveperson.common.domain.MemoryLayer;

import java.util.List;

/**
 * Mem0 三层记忆封装边界（Week1 提供 Stub，后续接真实 Server）。
 */
public interface MemoryService {

    void add(String userId, MemoryLayer layer, String content);

    List<String> search(String userId, MemoryLayer layer, String query, int limit);

    void updatePreference(String userId, String key, String value);
}
