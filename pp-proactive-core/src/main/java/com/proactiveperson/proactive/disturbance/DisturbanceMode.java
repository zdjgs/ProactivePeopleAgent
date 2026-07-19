package com.proactiveperson.proactive.disturbance;

/**
 * 防干扰三模式（设计方案：完全静默 / 仅重要 / 自定义时段）+ 正常。
 */
public enum DisturbanceMode {
    /** 正常：不额外限制（仍受日上限、忙碌度约束） */
    NORMAL,
    /** 完全静默：禁止一切主动推送 */
    QUIET,
    /** 仅重要：只放行高优先级（重要话题 / 任务截止前 24h） */
    IMPORTANT_ONLY,
    /** 自定义勿扰时段：时段内仅高优，时段外正常 */
    CUSTOM_HOURS
}
