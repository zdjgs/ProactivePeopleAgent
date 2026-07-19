package com.proactiveperson.proactive.disturbance;

import com.proactiveperson.memory.stub.InMemoryMemoryService;
import com.proactiveperson.proactive.config.DisturbanceProperties;
import com.proactiveperson.proactive.disturbance.cache.InMemoryTtlPreferenceCache;
import com.proactiveperson.proactive.user.ProactiveUser;
import com.proactiveperson.proactive.user.ProactiveUserRepository;
import com.proactiveperson.wechat.WeChatMessageGateway;
import com.proactiveperson.wechat.WeChatOutboundChannel;
import com.proactiveperson.wechat.inbound.InboundWeChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisturbanceCommandListenerTest {

    @Mock
    private ProactiveUserRepository userRepository;
    @Mock
    private WeChatMessageGateway weChatMessageGateway;

    private DisturbancePreferenceService preferenceService;
    private DisturbanceCommandListener listener;

    @BeforeEach
    void setUp() {
        DisturbanceProperties properties = new DisturbanceProperties();
        preferenceService = new DisturbancePreferenceService(
                new InMemoryMemoryService(), new InMemoryTtlPreferenceCache(), properties);
        listener = new DisturbanceCommandListener(userRepository, preferenceService, weChatMessageGateway);
    }

    @Test
    void handlesMuteTodayCommand() {
        ProactiveUser user = new ProactiveUser("u1", "oid", ZoneId.of("Asia/Shanghai"), true, "上海", false);
        when(userRepository.findByOpenId("oid")).thenReturn(Optional.of(user));
        when(weChatMessageGateway.sendTextAuto(anyString(), anyString()))
                .thenReturn(WeChatMessageGateway.SendResult.ok(WeChatOutboundChannel.CUSTOMER_SERVICE, "m1"));

        listener.onInbound(new InboundWeChatMessage("oid", "text", "今天别打扰", Instant.now()));

        assertThat(preferenceService.get("u1").mutedUntil()).isNotNull();
        ArgumentCaptor<String> reply = ArgumentCaptor.forClass(String.class);
        verify(weChatMessageGateway).sendTextAuto(eq("oid"), reply.capture());
        assertThat(reply.getValue()).contains("今天不再主动打扰");
    }

    @Test
    void handlesQuietModeCommand() {
        ProactiveUser user = new ProactiveUser("u1", "oid", ZoneId.of("Asia/Shanghai"), true, "上海", false);
        when(userRepository.findByOpenId("oid")).thenReturn(Optional.of(user));
        when(weChatMessageGateway.sendTextAuto(anyString(), anyString()))
                .thenReturn(WeChatMessageGateway.SendResult.ok(WeChatOutboundChannel.CUSTOMER_SERVICE, "m1"));

        listener.onInbound(new InboundWeChatMessage("oid", "text", "开启安静模式", Instant.now()));

        assertThat(preferenceService.get("u1").mode()).isEqualTo(DisturbanceMode.QUIET);
    }
}
