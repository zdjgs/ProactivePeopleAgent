package com.proactiveperson.memory.mem0;

import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.memory.config.MemoryProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Mem0MemoryServiceTest {

    private MockWebServer server;
    private Mem0MemoryService service;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        MemoryProperties properties = new MemoryProperties();
        properties.getMem0().setMode("platform");
        properties.getMem0().setBaseUrl(server.url("/").toString());
        properties.getMem0().setApiKey("test-key");
        service = new Mem0MemoryService(new Mem0RestClient(properties));
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void updatePreferenceWritesLongTermMemoryOnPlatform() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"results\":[]}"));

        service.updatePreference("u1", "quiet_mode", "on");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/v3/memories/add/");
        assertThat(request.getHeader("Authorization")).isEqualTo("Token test-key");
        assertThat(request.getBody().readUtf8())
                .contains("\"pp_layer\":\"LONG_TERM\"")
                .contains("\"pp_pref_key\":\"quiet_mode\"")
                .contains("preference:quiet_mode=on");
    }
}
