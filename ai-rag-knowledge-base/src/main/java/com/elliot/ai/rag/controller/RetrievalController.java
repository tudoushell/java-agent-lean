package com.elliot.ai.rag.controller;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.rag.dto.RetrievalSearchDto;
import com.elliot.ai.rag.dto.RetrievalSearchResultDto;
import com.elliot.ai.rag.service.RetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库向量检索接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/retrieval")
@Tag(name = "知识库检索", description = "基于查询文本从指定知识库召回相关片段")
public class RetrievalController {

    private final RetrievalService retrievalService;

    /**
     * 在指定知识库中执行相似度检索。
     *
     * @param searchDto 知识库 ID、查询文本和检索参数
     * @return 命中的文本片段及其相似度分数
     */
    @PostMapping("/search")
    @Operation(summary = "检索知识库", description = "根据查询文本和 metadata 过滤条件召回最相关的文档片段。")
    public Result<RetrievalSearchResultDto> search(
            @Valid @RequestBody RetrievalSearchDto searchDto
    ) {
        return Result.buildSuccess(retrievalService.search(searchDto));
    }
}
