package com.proactiveperson.wechat;

/**
 * 微信消息网关边界：客服消息（48h）与后续模板/订阅消息。
 */
public interface WeChatMessageGateway {

    /**
     * 发送客服文本消息（需在 48 小时互动窗口内）。
     */
    SendResult sendCustomerText(String openId, String content);

    record SendResult(boolean success, String messageId, String detail) {
        public static SendResult ok(String messageId) {
            return new SendResult(true, messageId, "ok");
        }

        public static SendResult fail(String detail) {
            return new SendResult(false, null, detail);
        }
    }
}
