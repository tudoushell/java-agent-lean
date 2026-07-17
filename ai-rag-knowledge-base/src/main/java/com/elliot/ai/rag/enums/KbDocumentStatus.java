package com.elliot.ai.rag.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 与 kb_document.status 的数据库约束保持一致。 */
@Getter
@RequiredArgsConstructor
public enum KbDocumentStatus {

    UPLOADED("UPLOADED"),
    PARSED("PARSED"),
    FAILED("FAILED"),
    CHUNKED("CHUNKED");

    @EnumValue
    private final String value;
}
