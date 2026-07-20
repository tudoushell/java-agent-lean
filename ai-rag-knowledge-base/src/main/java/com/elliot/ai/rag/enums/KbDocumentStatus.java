package com.elliot.ai.rag.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 与 kb_document.status 的数据库约束保持一致。
 */
@Getter
@RequiredArgsConstructor
public enum KbDocumentStatus {

    /** 文件已上传，等待解析。 */
    UPLOADED("UPLOADED"),
    /** 文件已解析为标准文本，等待切分。 */
    PARSED("PARSED"),
    /** 文件解析或切分失败。 */
    FAILED("FAILED"),
    /** 文本已切分为 Chunk，等待生成向量。 */
    CHUNKED("CHUNKED"),
    /** 正在生成向量并写入向量库。 */
    INDEXING("INDEXING"),
    /** 向量已成功写入向量库，可用于检索。 */
    INDEXED("INDEXED"),
    /** 生成向量或写入向量库失败。 */
    INDEX_FAILED("INDEX_FAILED");


    /** 持久化到 {@code kb_document.status} 的枚举值。 */
    @EnumValue
    private final String value;
}
