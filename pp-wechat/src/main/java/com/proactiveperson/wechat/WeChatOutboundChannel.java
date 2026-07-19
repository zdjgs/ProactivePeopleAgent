package com.proactiveperson.wechat;

/**
 * 出站通道：48h 内客服消息；超窗模板/订阅消息降级。
 */
public enum WeChatOutboundChannel {
    /** 客服消息（需用户 48h 内有过互动） */
    CUSTOMER_SERVICE,
    /** 模板消息（需后台申请模板，可绕过 48h） */
    TEMPLATE,
    /** 订阅消息（小程序场景，需用户订阅） */
    SUBSCRIBE
}
