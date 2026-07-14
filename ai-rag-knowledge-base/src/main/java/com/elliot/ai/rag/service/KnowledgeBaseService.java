package com.elliot.ai.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elliot.ai.rag.entity.KnowledgeBase;

/** 知识库业务服务。 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    void create(String name, String description);
}
