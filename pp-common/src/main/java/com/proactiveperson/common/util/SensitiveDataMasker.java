package com.proactiveperson.common.util;

import org.springframework.util.StringUtils;

public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    public static String maskSecret(String value) {
        if (!StringUtils.hasText(value)) {
            return "(empty)";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
