package com.proactiveperson.agent;

import com.proactiveperson.common.domain.MemoryLayer;
import com.proactiveperson.common.exception.LlmInvocationException;
import com.proactiveperson.memory.MemoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultChatServiceTest {

    @Mock
    private Assistant assistant;

    @Mock
    private MemoryService memoryService;

    @InjectMocks
    private DefaultChatService chatService;

    @Test
    void chatPersistsShortTermMemoryAfterSuccessfulReply() {
        when(assistant.chat("session-1", "今天有点累")).thenReturn("先歇会儿，我陪你。");

        ChatService.ChatResult result = chatService.chat("u1", "session-1", "今天有点累");

        assertThat(result.reply()).isEqualTo("先歇会儿，我陪你。");
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(memoryService).add(eq("u1"), eq(MemoryLayer.SHORT_TERM), contentCaptor.capture());
        assertThat(contentCaptor.getValue()).contains("user: 今天有点累");
        assertThat(contentCaptor.getValue()).contains("assistant: 先歇会儿，我陪你。");
    }

    @Test
    void chatUsesAnonymousUserWhenUserIdMissing() {
        when(assistant.chat("session-2", "hi")).thenReturn("你好");

        chatService.chat(null, "session-2", "hi");

        verify(memoryService).add(eq("anonymous"), eq(MemoryLayer.SHORT_TERM), any());
    }

    @Test
    void chatWrapsLlmFailure() {
        when(assistant.chat("session-3", "fail")).thenThrow(new RuntimeException("upstream timeout"));

        assertThatThrownBy(() -> chatService.chat("u1", "session-3", "fail"))
                .isInstanceOf(LlmInvocationException.class)
                .hasMessageContaining("模型调用失败");
    }
}
