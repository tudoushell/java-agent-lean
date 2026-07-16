package com.elliot.ai.rag.controller;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.rag.dto.KbDocumentDto;
import com.elliot.ai.rag.service.KbDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 知识库文档管理接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}/documents")
@Tag(name = "知识库文档管理", description = "知识库文档的上传与维护接口")
public class KbDocumentController {

    private final KbDocumentService kbDocumentService;

    /**
     * 向指定知识库上传文档。
     *
     * @param knowledgeBaseId 目标知识库 ID
     * @param file            待上传的 TXT 或 Markdown 文件
     * @return 已创建的知识库文档信息
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传知识库文档", description = "向指定知识库上传 TXT 或 Markdown 文档。")
    public Result<KbDocumentDto> upload(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "目标知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(description = "文件") @RequestPart("file") MultipartFile file
    ) {
        return Result.buildSuccess(kbDocumentService.upload(knowledgeBaseId, file));
    }
}
