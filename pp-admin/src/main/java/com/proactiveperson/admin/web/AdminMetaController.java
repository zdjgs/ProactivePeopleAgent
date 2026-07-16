package com.proactiveperson.admin.web;

import com.proactiveperson.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminMetaController {

    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> meta() {
        return ApiResponse.ok(Map.of(
                "module", "admin-console",
                "status", "skeleton",
                "capabilities", new String[]{"rules", "prompts", "skills"}
        ));
    }
}
