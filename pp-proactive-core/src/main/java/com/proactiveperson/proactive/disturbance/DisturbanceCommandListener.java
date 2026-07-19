package com.proactiveperson.proactive.disturbance;

import com.proactiveperson.proactive.user.ProactiveUser;
import com.proactiveperson.proactive.user.ProactiveUserRepository;
import com.proactiveperson.wechat.WeChatMessageGateway;
import com.proactiveperson.wechat.inbound.InboundWeChatMessage;
import com.proactiveperson.wechat.inbound.WeChatInboundListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * 微信文本快捷指令：今天别打扰 / 模式切换。
 */
@Component
public class DisturbanceCommandListener implements WeChatInboundListener {

    private static final Logger log = LoggerFactory.getLogger(DisturbanceCommandListener.class);

    private final ProactiveUserRepository userRepository;
    private final DisturbancePreferenceService preferenceService;
    private final WeChatMessageGateway weChatMessageGateway;

    public DisturbanceCommandListener(ProactiveUserRepository userRepository,
                                      DisturbancePreferenceService preferenceService,
                                      WeChatMessageGateway weChatMessageGateway) {
        this.userRepository = userRepository;
        this.preferenceService = preferenceService;
        this.weChatMessageGateway = weChatMessageGateway;
    }

    @Override
    public void onInbound(InboundWeChatMessage message) {
        Optional<String> text = message.textContent();
        if (text.isEmpty()) {
            return;
        }
        String command = normalize(text.get());
        Optional<Command> matched = Command.match(command);
        if (matched.isEmpty()) {
            return;
        }

        Optional<ProactiveUser> userOpt = userRepository.findByOpenId(message.openId());
        if (userOpt.isEmpty()) {
            log.info("disturbance command ignored: unknown openId={}", message.openId());
            return;
        }

        ProactiveUser user = userOpt.get();
        String reply = apply(user, matched.get());
        WeChatMessageGateway.SendResult send = weChatMessageGateway.sendTextAuto(user.openId(), reply);
        log.info("disturbance command applied userId={} cmd={} sendOk={}",
                user.userId(), matched.get(), send.success());
    }

    private String apply(ProactiveUser user, Command command) {
        return switch (command) {
            case MUTE_TODAY -> {
                preferenceService.muteForRestOfDay(user.userId(), user.timezone());
                yield "好的，今天不再主动打扰你。需要时随时找我。";
            }
            case QUIET -> {
                preferenceService.setMode(user.userId(), DisturbanceMode.QUIET);
                yield "已开启安静模式：暂停全部主动推送。回复「关闭防干扰」可恢复。";
            }
            case IMPORTANT_ONLY -> {
                preferenceService.setMode(user.userId(), DisturbanceMode.IMPORTANT_ONLY);
                yield "已开启仅重要模式：只推送高优先级消息。回复「关闭防干扰」可恢复。";
            }
            case NORMAL -> {
                preferenceService.clearMute(user.userId());
                preferenceService.setMode(user.userId(), DisturbanceMode.NORMAL);
                yield "已关闭防干扰，恢复正常主动关怀。";
            }
        };
    }

    private static String normalize(String raw) {
        return raw.trim()
                .replace("“", "")
                .replace("”", "")
                .replace("「", "")
                .replace("」", "")
                .toLowerCase(Locale.ROOT);
    }

    enum Command {
        MUTE_TODAY,
        QUIET,
        IMPORTANT_ONLY,
        NORMAL;

        static Optional<Command> match(String normalized) {
            if (normalized.contains("今天别打扰") || normalized.equals("别打扰") || normalized.equals("勿扰今天")) {
                return Optional.of(MUTE_TODAY);
            }
            if (normalized.contains("开启安静") || normalized.equals("安静模式") || normalized.contains("完全静默")) {
                return Optional.of(QUIET);
            }
            if (normalized.contains("仅重要") || normalized.contains("只要重要")) {
                return Optional.of(IMPORTANT_ONLY);
            }
            if (normalized.contains("关闭防干扰") || normalized.equals("恢复正常") || normalized.contains("关闭勿扰")) {
                return Optional.of(NORMAL);
            }
            return Optional.empty();
        }
    }
}
