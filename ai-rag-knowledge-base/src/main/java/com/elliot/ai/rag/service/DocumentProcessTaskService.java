package com.elliot.ai.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elliot.ai.rag.entity.DocumentProcessTask;

import java.util.UUID;

/** 文档处理任务业务服务。 */
public interface DocumentProcessTaskService extends IService<DocumentProcessTask> {
    DocumentProcessTask createFullProcessTask(UUID documentId);
}
