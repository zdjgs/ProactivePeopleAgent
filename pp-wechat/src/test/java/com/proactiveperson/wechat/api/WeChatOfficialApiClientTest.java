package com.proactiveperson.wechat.api;

import com.proactiveperson.wechat.config.WeChatProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

class WeChatOfficialApiClientTest {

    private MockWebServer server;
    private WeChatOfficialApiClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        WeChatProperties properties = new WeChatProperties();
        properties.setAppId("wx_app");
        properties.setAppSecret("wx_secret");
        properties.setApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        properties.setTemplateId("tpl_001");
        properties.setTemplateContentKey("thing1");
        properties.setTimeoutSeconds(5);

        client = WeChatOfficialApiClient.createForTest(properties, HttpClient.newHttpClient());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void sendCustomerTextFetchesTokenThenPosts() throws Exception {
        server.enqueue(new MockResponse().setBody("""
                {"access_token":"token-1","expires_in":7200}
                """));
        server.enqueue(new MockResponse().setBody("""
                {"errcode":0,"errmsg":"ok","msgid":"mid-1"}
                """));

        String messageId = client.sendCustomerText("openid_1", "你好");

        assertThat(messageId).isEqualTo("mid-1");
        RecordedRequest tokenReq = server.takeRequest();
        assertThat(tokenReq.getPath()).contains("/cgi-bin/token");
        RecordedRequest sendReq = server.takeRequest();
        assertThat(sendReq.getPath()).contains("/cgi-bin/message/custom/send");
        assertThat(sendReq.getBody().readUtf8()).contains("\"touser\":\"openid_1\"").contains("你好");
    }

    @Test
    void sendTemplateTextUsesConfiguredTemplate() throws Exception {
        server.enqueue(new MockResponse().setBody("""
                {"access_token":"token-2","expires_in":7200}
                """));
        server.enqueue(new MockResponse().setBody("""
                {"errcode":0,"errmsg":"ok","msgid":"tpl-mid"}
                """));

        String messageId = client.sendTemplateText("openid_2", "主动关怀内容");

        assertThat(messageId).isEqualTo("tpl-mid");
        server.takeRequest(); // token
        RecordedRequest sendReq = server.takeRequest();
        assertThat(sendReq.getPath()).contains("/cgi-bin/message/template/send");
        assertThat(sendReq.getBody().readUtf8())
                .contains("\"template_id\":\"tpl_001\"")
                .contains("\"thing1\"");
    }
}
