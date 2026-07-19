package com.proactiveperson.wechat;

/**
 * 微信消息网关：客服消息（48h）+ 模板/订阅降级。
 * <p>
 * 完整主动推送安全自检与审计见 T-009 / REQ-013。
 */
public interface WeChatMessageGateway {

    /**
     * 发送客服文本（调用方须确保仍在 48h 互动窗口内，或先查 {@link #resolveChannel}）。
     */
    SendResult sendCustomerText(String openId, String content);

    /**
     * 发送模板文本（超窗降级路径；模板 ID 来自配置）。
     */
    SendResult sendTemplateText(String openId, String content);

    /**
     * 按 48h 窗口自动选择通道：窗口内客服，窗口外模板（未配置模板则失败并说明降级策略）。
     */
    SendResult sendTextAuto(String openId, String content);

    /**
     * 解析应对当前 openId 使用的出站通道。
     */
    WeChatOutboundChannel resolveChannel(String openId);

    record SendResult(boolean success, WeChatOutboundChannel channel, String messageId, String detail) {

        public static SendResult ok(WeChatOutboundChannel channel, String messageId) {
            return new SendResult(true, channel, messageId, "ok");
        }

        public static SendResult fail(WeChatOutboundChannel channel, String detail) {
            return new SendResult(false, channel, null, detail);
        }
    }
}
