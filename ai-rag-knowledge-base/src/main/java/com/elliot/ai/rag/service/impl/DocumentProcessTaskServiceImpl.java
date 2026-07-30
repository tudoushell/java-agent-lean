package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.entity.DocumentProcessTask;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.enums.DocumentTaskStatus;
import com.elliot.ai.rag.enums.DocumentTaskStep;
import com.elliot.ai.rag.enums.KbDocumentStatus;
import com.elliot.ai.rag.mapper.DocumentProcessTaskMapper;
import com.elliot.ai.rag.mapper.KbDocumentMapper;
import com.elliot.ai.rag.service.DocumentProcessTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 文档处理任务业务服务实现。
 */
@RequiredArgsConstructor
@Service
public class DocumentProcessTaskServiceImpl
        extends ServiceImpl<DocumentProcessTaskMapper, DocumentProcessTask>
        implements DocumentProcessTaskService {

    private final KbDocumentMapper documentMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DocumentProcessTask createFullProcessTask(UUID documentId) {
        KbDocument kbDocument = documentMapper.selectById(documentId);
        if (kbDocument == null) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在");
        }
        DocumentProcessTask activeTask = getActiveTask(documentId);
        if (activeTask != null) {
            return activeTask;
        }

        DocumentProcessTask task = new DocumentProcessTask();
        task.setId(UUID.randomUUID());
        task.setDocumentId(kbDocument.getId());
        task.setTaskType("FULL_PROCESS");
        task.setStatus(DocumentTaskStatus.PENDING);
        task.setCurrentStep(DocumentTaskStep.PARSE);
        task.setProgress(0);
        task.setRetryCount(0);
        task.setMaxRetries(3);
        try {
            this.save(task);
        } catch (DuplicateKeyException e) {
            DocumentProcessTask existing = getActiveTask(kbDocument.getId());
            if (existing != null) {
                return existing;
            }
            throw e;
        }
        kbDocument.setStatus(KbDocumentStatus.QUEUED);
        kbDocument.setErrorMessage(null);
        documentMapper.updateById(kbDocument);
        return task;
    }

    private DocumentProcessTask getActiveTask(UUID documentId) {
        return this.baseMapper.selectOne(new LambdaQueryWrapper<DocumentProcessTask>().
                eq(DocumentProcessTask::getDocumentId, documentId)
                .in(DocumentProcessTask::getStatus, DocumentTaskStatus.listActiveStatuses())
                .last("limit 1")
        );
    }
}
