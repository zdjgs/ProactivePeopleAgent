package com.proactiveperson.wechat.official;

import com.proactiveperson.common.state.InMemoryStateStore;
import com.proactiveperson.wechat.WeChatMessageGateway;
import com.proactiveperson.wechat.WeChatOutboundChannel;
import com.proactiveperson.wechat.api.WeChatOfficialApiClient;
import com.proactiveperson.wechat.config.WeChatProperties;
import com.proactiveperson.wechat.window.CustomerServiceWindowTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficialWeChatMessageGatewayTest {

    @Mock
    private WeChatOfficialApiClient apiClient;

    private WeChatProperties properties;
    private CustomerServiceWindowTracker windowTracker;
    private OfficialWeChatMessageGateway gateway;

    @BeforeEach
    void setUp() {
        properties = new WeChatProperties();
        properties.setCustomerServiceWindowHours(48);
        properties.setTemplateId("tpl");
        windowTracker = new CustomerServiceWindowTracker(properties, new InMemoryStateStore());
        gateway = new OfficialWeChatMessageGateway(apiClient, windowTracker, properties);
    }

    @Test
    void autoUsesCustomerServiceInsideWindow() {
        windowTracker.recordInbound("o1", Instant.now());
        when(apiClient.sendCustomerText("o1", "hi")).thenReturn("mid");

        WeChatMessageGateway.SendResult result = gateway.sendTextAuto("o1", "hi");

        assertThat(result.success()).isTrue();
        assertThat(result.channel()).isEqualTo(WeChatOutboundChannel.CUSTOMER_SERVICE);
        verify(apiClient).sendCustomerText("o1", "hi");
        verify(apiClient, never()).sendTemplateText("o1", "hi");
    }

    @Test
    void autoFallsBackToTemplateOutsideWindow() {
        when(apiClient.sendTemplateText("o1", "hi")).thenReturn("tpl-mid");

        WeChatMessageGateway.SendResult result = gateway.sendTextAuto("o1", "hi");

        assertThat(result.success()).isTrue();
        assertThat(result.channel()).isEqualTo(WeChatOutboundChannel.TEMPLATE);
        verify(apiClient).sendTemplateText("o1", "hi");
    }

    @Test
    void autoFailsOutsideWindowWithoutTemplateId() {
        properties.setTemplateId("");

        WeChatMessageGateway.SendResult result = gateway.sendTextAuto("o1", "hi");

        assertThat(result.success()).isFalse();
        assertThat(result.detail()).contains("48h");
        verify(apiClient, never()).sendCustomerText("o1", "hi");
    }
}
