package com.proactiveperson.common.domain;

/**
 * Mem0 三层记忆语义。
 */
public enum MemoryLayer {
    /** 本次会话 */
    SHORT_TERM,
    /** 本周/近期总结 */
    MID_TERM,
    /** 画像、偏好、重要事实 */
    LONG_TERM
}
