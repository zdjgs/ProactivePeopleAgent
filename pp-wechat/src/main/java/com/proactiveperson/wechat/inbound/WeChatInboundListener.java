package com.proactiveperson.wechat.inbound;

/**
 * 微信入站消息监听（业务模块实现，如防干扰指令）。
 */
@FunctionalInterface
public interface WeChatInboundListener {

    void onInbound(InboundWeChatMessage message);
}
