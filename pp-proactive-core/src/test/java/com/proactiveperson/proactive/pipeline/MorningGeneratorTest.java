package com.proactiveperson.proactive.pipeline;

import com.proactiveperson.proactive.location.LocationContextResolver;
import com.proactiveperson.proactive.user.ProactiveUser;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MorningGeneratorTest {

    @Test
    void templateIncludesCityAndQuestion() {
        ProactiveUser user = new ProactiveUser("u1", "o1", ZoneId.of("Asia/Shanghai"), true, "上海", false);
        ResearchBrief research = new ResearchBrief(
                user,
                LocationContextResolver.LocationContext.authorized("上海"),
                List.of("阳光正好"),
                false);
        PersonalizedBrief brief = new PersonalizedBrief(research, List.of("喜欢慢跑"), "温暖");

        String message = MorningGenerator.buildTemplateMessage(brief);

        assertThat(message).contains("上海").contains("阳光正好").contains("慢跑").contains("？");
    }
}
