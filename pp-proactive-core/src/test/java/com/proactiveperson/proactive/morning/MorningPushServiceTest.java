package com.proactiveperson.proactive.morning;

import com.proactiveperson.common.state.InMemoryStateStore;
import com.proactiveperson.mcp.McpToolClient;
import com.proactiveperson.memory.MemoryService;
import com.proactiveperson.memory.stub.InMemoryMemoryService;
import com.proactiveperson.monitor.ActivityStatus;
import com.proactiveperson.proactive.config.DisturbanceProperties;
import com.proactiveperson.proactive.config.ProactiveProperties;
import com.proactiveperson.proactive.disturbance.DisturbancePreferenceService;
import com.proactiveperson.proactive.disturbance.cache.InMemoryTtlPreferenceCache;
import com.proactiveperson.proactive.gate.MorningPushGate;
import com.proactiveperson.proactive.location.LocationContextResolver;
import com.proactiveperson.proactive.pipeline.MorningGenerator;
import com.proactiveperson.proactive.pipeline.MorningPersonalizer;
import com.proactiveperson.proactive.pipeline.MorningResearcher;
import com.proactiveperson.proactive.quota.DailyPushQuotaService;
import com.proactiveperson.proactive.user.ProactiveUser;
import com.proactiveperson.proactive.window.MorningPushWindowService;
import com.proactiveperson.wechat.WeChatMessageGateway;
import com.proactiveperson.wechat.WeChatOutboundChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MorningPushServiceTest {

    @Mock
    private WeChatMessageGateway weChatMessageGateway;
    @Mock
    private MemoryService memoryService;
    @Mock
    private McpToolClient mcpToolClient;
    @Mock
    private ObjectProvider<com.proactiveperson.agent.Assistant> assistantProvider;

    private ProactiveProperties properties;
    private DailyPushQuotaService quotaService;
    private DisturbanceProperties disturbanceProperties;
    private MorningPushService service;

    @BeforeEach
    void setUp() {
        properties = new ProactiveProperties();
        properties.setDailyPushLimit(2);
        properties.setMorningWindowStartHour(8);
        properties.setMorningWindowEndHour(10);
        quotaService = new DailyPushQuotaService(properties, new InMemoryStateStore());
        MorningPushWindowService windowService = new MorningPushWindowService(properties);
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T01:15:00Z"), ZoneOffset.UTC); // 09:15 CST
        DisturbancePreferenceService preferenceService = new DisturbancePreferenceService(
                new InMemoryMemoryService(), new InMemoryTtlPreferenceCache(), new DisturbanceProperties());
        MorningPushGate gate = MorningPushGate.createForTest(
                windowService, quotaService, properties, preferenceService,
                userId -> ActivityStatus.IDLE, clock);

        disturbanceProperties = new DisturbanceProperties();
        lenient().when(assistantProvider.getIfAvailable()).thenReturn(null);

        service = new MorningPushService(
                gate,
                new LocationContextResolver(),
                new MorningResearcher(mcpToolClient),
                new MorningPersonalizer(memoryService),
                new MorningGenerator(assistantProvider),
                weChatMessageGateway,
                quotaService,
                disturbanceProperties);
    }

    @Test
    void pushesThroughPipelineAndRecordsQuota() {
        ProactiveUser user = new ProactiveUser("u1", "openid-1", ZoneId.of("Asia/Shanghai"), true, "上海", false);
        when(mcpToolClient.callTool(anyString(), any())).thenAnswer(inv -> {
            String tool = inv.getArgument(0);
            if ("weather".equals(tool)) {
                return Map.of("summary", "上海今天多云转晴。");
            }
            return Map.of("summary", "一条轻松的早间资讯。");
        });
        when(memoryService.search(anyString(), any(), anyString(), any(Integer.class)))
                .thenReturn(List.of("喜欢早晨咖啡"));
        when(weChatMessageGateway.sendTextAuto(anyString(), anyString()))
                .thenReturn(WeChatMessageGateway.SendResult.ok(WeChatOutboundChannel.CUSTOMER_SERVICE, "mid-1"));

        MorningPushService.PushOutcome outcome = service.pushForUser(user);

        assertThat(outcome.sent()).isTrue();
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(weChatMessageGateway).sendTextAuto(org.mockito.ArgumentMatchers.eq("openid-1"), content.capture());
        assertThat(content.getValue()).contains("上海");
        assertThat(content.getValue()).contains("今天别打扰");
        assertThat(quotaService.currentCount(user,
                java.time.ZonedDateTime.ofInstant(Instant.parse("2026-07-19T01:15:00Z"), ZoneId.of("Asia/Shanghai"))))
                .isEqualTo(1);
    }

    @Test
    void skipsWhenDoNotDisturb() {
        ProactiveUser user = new ProactiveUser("u1", "openid-1", ZoneId.of("Asia/Shanghai"), true, "上海", true);

        MorningPushService.PushOutcome outcome = service.pushForUser(user);

        assertThat(outcome.skipped()).isTrue();
        assertThat(outcome.code()).isEqualTo("QUIET_MODE");
        verify(weChatMessageGateway, never()).sendTextAuto(anyString(), anyString());
    }

    @Test
    void degradesLocationButStillPushes() {
        ProactiveUser user = new ProactiveUser("u1", "openid-1", ZoneId.of("Asia/Shanghai"), false, null, false);
        when(mcpToolClient.callTool(anyString(), any())).thenReturn(Map.of("status", "stub-not-configured"));
        when(memoryService.search(anyString(), any(), anyString(), any(Integer.class))).thenReturn(List.of());
        when(weChatMessageGateway.sendTextAuto(anyString(), anyString()))
                .thenReturn(WeChatMessageGateway.SendResult.ok(WeChatOutboundChannel.TEMPLATE, "mid-2"));

        MorningPushService.PushOutcome outcome = service.pushForUser(user);

        assertThat(outcome.sent()).isTrue();
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(weChatMessageGateway).sendTextAuto(org.mockito.ArgumentMatchers.eq("openid-1"), content.capture());
        assertThat(content.getValue()).contains("新的一天");
    }

    @Test
    void pushesOnlyOncePerLocalDayEvenIfScannedAgain() {
        ProactiveUser user = new ProactiveUser("u1", "openid-1", ZoneId.of("Asia/Shanghai"), true, "上海", false);
        when(mcpToolClient.callTool(anyString(), any())).thenReturn(Map.of("summary", "素材"));
        when(memoryService.search(anyString(), any(), anyString(), any(Integer.class))).thenReturn(List.of());
        when(weChatMessageGateway.sendTextAuto(anyString(), anyString()))
                .thenReturn(WeChatMessageGateway.SendResult.ok(WeChatOutboundChannel.CUSTOMER_SERVICE, "mid-1"));

        assertThat(service.pushForUser(user).sent()).isTrue();
        MorningPushService.PushOutcome second = service.pushForUser(user);

        assertThat(second.skipped()).isTrue();
        assertThat(second.code()).isEqualTo("ALREADY_MORNING_TODAY");
        verify(weChatMessageGateway, times(1)).sendTextAuto(anyString(), anyString());
    }

    @Test
    void releasesReservationWhenSendFails() {
        ProactiveUser user = new ProactiveUser("u1", "openid-1", ZoneId.of("Asia/Shanghai"), true, "上海", false);
        when(mcpToolClient.callTool(anyString(), any())).thenReturn(Map.of("summary", "素材"));
        when(memoryService.search(anyString(), any(), anyString(), any(Integer.class))).thenReturn(List.of());
        when(weChatMessageGateway.sendTextAuto(anyString(), anyString()))
                .thenReturn(WeChatMessageGateway.SendResult.fail(WeChatOutboundChannel.TEMPLATE, "down"))
                .thenReturn(WeChatMessageGateway.SendResult.ok(WeChatOutboundChannel.CUSTOMER_SERVICE, "mid-ok"));

        assertThat(service.pushForUser(user).code()).isEqualTo("SEND_FAILED");
        assertThat(service.pushForUser(user).sent()).isTrue();
        verify(weChatMessageGateway, times(2)).sendTextAuto(anyString(), anyString());
    }
}
