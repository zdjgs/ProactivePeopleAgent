package com.proactiveperson.proactive.location;

import com.proactiveperson.proactive.user.ProactiveUser;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class LocationContextResolverTest {

    private final LocationContextResolver resolver = new LocationContextResolver();

    @Test
    void authorizedReturnsCity() {
        ProactiveUser user = new ProactiveUser("u1", "o1", ZoneId.of("Asia/Shanghai"), true, "上海", false);
        var ctx = resolver.resolve(user);
        assertThat(ctx.authorized()).isTrue();
        assertThat(ctx.cityOrLabel()).isEqualTo("上海");
    }

    @Test
    void unauthorizedDegrades() {
        ProactiveUser user = new ProactiveUser("u1", "o1", ZoneId.of("Asia/Shanghai"), false, "上海", false);
        var ctx = resolver.resolve(user);
        assertThat(ctx.authorized()).isFalse();
        assertThat(ctx.cityOrLabel()).isEqualTo("通用");
        assertThat(ctx.note()).contains("未授权");
    }
}
