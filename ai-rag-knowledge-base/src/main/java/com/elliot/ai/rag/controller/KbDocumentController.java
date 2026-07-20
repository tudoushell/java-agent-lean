package com.elliot.ai.rag.controller;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.dto.IndexResultDto;
import com.elliot.ai.rag.dto.KbDocumentDto;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.service.DocumentChunkService;
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
    private final DocumentChunkService documentChunkService;

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

    /**
     * 将已解析的文档按 Token 切分为多个文本片段。
     *
     * @param knowledgeBaseId 所属知识库 ID
     * @param documentId      待切分的文档 ID
     * @return 实际生成并保存的 Chunk 数量
     */
    @PostMapping("/{documentId}/chunks")
    @Operation(summary = "切分知识库文档", description = "读取已解析文本，按 Token 切分并保存文档片段。")
    public Result<Integer> chunk(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "所属知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(
                    name = "documentId",
                    description = "待切分的文档 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("documentId") UUID documentId
    ) {
        KbDocument document = kbDocumentService.getById(documentId);
        if (document == null || !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在或不属于该知识库");
        }
        return Result.buildSuccess(documentChunkService.chunk(documentId));
    }

    /**
     * 为已切分的文档生成向量并写入向量库。
     *
     * @param knowledgeBaseId 所属知识库 ID
     * @param documentId      待索引的文档 ID
     * @return 向量写入结果
     */
    @PostMapping("/{documentId}/index")
    @Operation(summary = "索引知识库文档", description = "为已切分的文本生成向量，并写入向量数据库。")
    public Result<IndexResultDto> index(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "所属知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(
                    name = "documentId",
                    description = "待索引的文档 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("documentId") UUID documentId
    ) {
        KbDocument document = kbDocumentService.getById(documentId);
        if (document == null || !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在或不属于该知识库");
        }
        return Result.buildSuccess(kbDocumentService.index(documentId));
    }
}
