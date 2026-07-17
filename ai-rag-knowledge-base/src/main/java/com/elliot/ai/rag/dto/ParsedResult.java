package com.elliot.ai.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ParsedResult {
    /**
     * 部分限制文本
     */
    private String limitContent;

    /** 解析结果中是否包含非空白字符。 */
    private boolean hasText;

    /** 解析结果的字符数。 */
    private long charCount;
}
