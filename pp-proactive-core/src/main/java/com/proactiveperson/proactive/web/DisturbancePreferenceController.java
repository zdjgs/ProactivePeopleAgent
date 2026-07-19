package com.proactiveperson.proactive.web;

import com.proactiveperson.proactive.disturbance.DisturbanceMode;
import com.proactiveperson.proactive.disturbance.DisturbancePreference;
import com.proactiveperson.proactive.disturbance.DisturbancePreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

/**
 * 防干扰偏好 REST（调试/后台用；微信侧走快捷指令）。
 */
@RestController
@RequestMapping("/api/preferences/disturbance")
public class DisturbancePreferenceController {

    private final DisturbancePreferenceService preferenceService;

    public DisturbancePreferenceController(DisturbancePreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<DisturbancePreferenceView> get(@RequestParam("userId") String userId) {
        if (!StringUtils.hasText(userId)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(DisturbancePreferenceView.from(preferenceService.get(userId)));
    }

    @PutMapping
    public ResponseEntity<DisturbancePreferenceView> put(@RequestParam("userId") String userId,
                                                         @RequestBody DisturbancePreferenceRequest request) {
        if (!StringUtils.hasText(userId) || request == null || request.mode() == null) {
            return ResponseEntity.badRequest().build();
        }
        DisturbancePreference preference = new DisturbancePreference(
                request.mode(),
                request.quietStart(),
                request.quietEnd(),
                request.mutedUntil(),
                request.importantTopics());
        preferenceService.save(userId, preference);
        return ResponseEntity.ok(DisturbancePreferenceView.from(preference));
    }

    public record DisturbancePreferenceRequest(
            DisturbanceMode mode,
            LocalTime quietStart,
            LocalTime quietEnd,
            Instant mutedUntil,
            Set<String> importantTopics
    ) {
    }

    public record DisturbancePreferenceView(
            DisturbanceMode mode,
            LocalTime quietStart,
            LocalTime quietEnd,
            Instant mutedUntil,
            Set<String> importantTopics
    ) {
        static DisturbancePreferenceView from(DisturbancePreference preference) {
            return new DisturbancePreferenceView(
                    preference.mode(),
                    preference.customQuietStart(),
                    preference.customQuietEnd(),
                    preference.mutedUntil(),
                    preference.importantTopics());
        }
    }
}
