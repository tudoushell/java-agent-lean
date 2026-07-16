package com.elliot.ai.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elliot.ai.rag.dto.KbDocumentDto;
import com.elliot.ai.rag.entity.KbDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** 知识库文档业务服务。 */
public interface KbDocumentService extends IService<KbDocument> {

    /**
     * 上传文件并创建知识库文档记录。
     *
     * @param knowledgeBaseId 目标知识库 ID
     * @param file 待上传的文件
     * @return 创建后的知识库文档信息
     */
    KbDocumentDto upload(UUID knowledgeBaseId, MultipartFile file);
}
