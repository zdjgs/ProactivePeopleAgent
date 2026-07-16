package com.proactiveperson.common.exception;

public class LlmNotEnabledException extends AppException {

    public static final String CODE = "LLM_NOT_ENABLED";

    public LlmNotEnabledException() {
        super(CODE, "LLM 未启用：请设置 pp.llm.enabled=true 并配置 OPENAI_API_KEY");
    }
}
