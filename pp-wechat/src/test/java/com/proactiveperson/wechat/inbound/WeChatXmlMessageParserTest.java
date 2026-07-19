package com.proactiveperson.wechat.inbound;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WeChatXmlMessageParserTest {

    private final WeChatXmlMessageParser parser = new WeChatXmlMessageParser();

    @Test
    void parsesTextMessage() {
        String xml = """
                <xml>
                <ToUserName><![CDATA[gh_server]]></ToUserName>
                <FromUserName><![CDATA[openid_abc]]></FromUserName>
                <CreateTime>1721111111</CreateTime>
                <MsgType><![CDATA[text]]></MsgType>
                <Content><![CDATA[今天别打扰]]></Content>
                <MsgId>123</MsgId>
                </xml>
                """;

        var msg = parser.parse(xml).orElseThrow();
        assertThat(msg.openId()).isEqualTo("openid_abc");
        assertThat(msg.isText()).isTrue();
        assertThat(msg.content()).isEqualTo("今天别打扰");
        assertThat(msg.createTime()).isEqualTo(Instant.ofEpochSecond(1721111111L));
    }
}
