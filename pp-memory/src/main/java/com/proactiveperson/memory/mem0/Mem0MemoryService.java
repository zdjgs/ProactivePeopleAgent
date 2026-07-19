package com.proactiveperson.memory.mem0;

import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.memory.MemoryService;
import com.proactiveperson.memory.config.MemoryProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "pp.memory.provider", havingValue = "mem0")
public class Mem0MemoryService implements MemoryService {

    private final Mem0RestClient client;

    public Mem0MemoryService(MemoryProperties properties) {
        this.client = new Mem0RestClient(properties);
    }

    Mem0MemoryService(Mem0RestClient client) {
        this.client = client;
    }

    @Override
    public void add(String userId, MemoryLayer layer, String content) {
        client.add(userId, layer, content, null);
    }

    @Override
    public List<String> search(String userId, MemoryLayer layer, String query, int limit) {
        return client.search(userId, layer, query, limit);
    }

    @Override
    public void updatePreference(String userId, String key, String value) {
        client.add(userId, MemoryLayer.LONG_TERM, "preference:" + key + "=" + value,
                Map.of(Mem0LayerMetadata.PREF_KEY, key));
    }
}
