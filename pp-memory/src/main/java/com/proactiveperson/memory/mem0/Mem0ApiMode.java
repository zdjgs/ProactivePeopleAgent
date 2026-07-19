package com.proactiveperson.memory.mem0;

public enum Mem0ApiMode {

    OSS,
    PLATFORM;

    public static Mem0ApiMode fromConfig(String mode) {
        if (mode == null) {
            return OSS;
        }
        return switch (mode.toLowerCase()) {
            case "platform", "cloud" -> PLATFORM;
            default -> OSS;
        };
    }
}
