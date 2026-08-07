package com.elliot.ai.rag.controller;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.dto.DocumentProcessTaskDto;
import com.elliot.ai.rag.entity.DocumentProcessTask;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.service.DocumentProcessTaskService;
import com.elliot.ai.rag.service.KbDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 文档异步处理任务接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/process-tasks")
@Tag(name = "文档处理任务", description = "创建并跟踪文档解析、切块、索引任务")
public class DocumentProcessTaskController {

    private final KbDocumentService kbDocumentService;
    private final DocumentProcessTaskService documentProcessTaskService;

    /**
     * 创建文档完整处理任务。
     *
     * <p>任务依次完成解析、切块和向量索引；文档已有活动任务时，
     * 返回已有任务而不会重复创建。</p>
     *
     * @param knowledgeBaseId 文档所属知识库 ID
     * @param documentId      待处理文档 ID
     * @return 新建或已存在的活动处理任务
     */
    @PostMapping
    @Operation(summary = "创建文档处理任务", description = "为文档创建解析、切块、索引的异步处理任务；重复提交会返回已有活动任务。")
    public Result<DocumentProcessTaskDto> createFullProcessTask(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "所属知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(
                    name = "documentId",
                    description = "待处理文档 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("documentId") UUID documentId
    ) {
        KbDocument document = kbDocumentService.getById(documentId);
        if (document == null || !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在或不属于该知识库");
        }
        return Result.buildSuccess(
                DocumentProcessTaskDto.from(documentProcessTaskService.createFullProcessTask(documentId))
        );
    }

    /**
     * 查询指定文档处理任务的实时进度。
     *
     * @param knowledgeBaseId 文档所属知识库 ID
     * @param documentId      文档 ID
     * @param taskId          文档处理任务 ID
     * @return 任务状态、当前步骤、进度及失败信息
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "查询文档处理进度", description = "查询文档处理任务的状态、当前步骤、进度和失败信息。")
    public Result<DocumentProcessTaskDto> getTaskProgress(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "所属知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(
                    name = "documentId",
                    description = "文档 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("documentId") UUID documentId,
            @Parameter(
                    name = "taskId",
                    description = "文档处理任务 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("taskId") UUID taskId
    ) {
        KbDocument document = kbDocumentService.getById(documentId);
        if (document == null || !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在或不属于该知识库");
        }
        DocumentProcessTask task = documentProcessTaskService.getById(taskId);
        if (task == null || !documentId.equals(task.getDocumentId())) {
            throw new BusinessException(ResultCode.FAIL, "处理任务不存在或不属于该文档");
        }
        return Result.buildSuccess(DocumentProcessTaskDto.from(task));
    }
}
