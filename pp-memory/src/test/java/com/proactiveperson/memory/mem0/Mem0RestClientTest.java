package com.proactiveperson.memory.mem0;

import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.memory.config.MemoryProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Mem0RestClientTest {

    private MockWebServer server;
    private Mem0RestClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        MemoryProperties properties = new MemoryProperties();
        properties.setProvider("mem0");
        properties.getMem0().setMode("oss");
        properties.getMem0().setBaseUrl(server.url("/").toString());
        properties.getMem0().setTimeoutSeconds(5);
        client = new Mem0RestClient(properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void addUsesOssEndpointWithLayerMetadata() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"results\":[]}"));

        client.add("u1", MemoryLayer.SHORT_TERM, "user: hi\nassistant: hello", null);

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/memories");
        assertThat(request.getBody().readUtf8())
                .contains("\"user_id\":\"u1\"")
                .contains("\"pp_layer\":\"SHORT_TERM\"")
                .contains("\"role\":\"user\"")
                .contains("\"content\":\"hi\"");
    }

    @Test
    void searchParsesOssResultsAndFiltersLayer() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("""
                {
                  "results": [
                    {"memory": "短期记忆", "metadata": {"pp_layer": "SHORT_TERM"}},
                    {"memory": "长期画像", "metadata": {"pp_layer": "LONG_TERM"}},
                    {"memory": "无分层应丢弃"}
                  ]
                }
                """));

        List<String> hits = client.search("u1", MemoryLayer.SHORT_TERM, "记忆", 5);

        assertThat(hits).containsExactly("短期记忆");
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/search");
    }

    @Test
    void parseMessagesFallsBackToSingleUserMessage() {
        List<java.util.Map<String, String>> messages = Mem0RestClient.parseMessages("plain text");
        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().get("content")).isEqualTo("plain text");
    }
}
