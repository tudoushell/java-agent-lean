package com.elliot.ai.rag.service;

import com.elliot.ai.rag.dto.RetrievalSearchDto;
import com.elliot.ai.rag.dto.RetrievalSearchResultDto;
import org.springframework.ai.document.Document;

public interface RetrievalService {

    /**
     * 在指定知识库内执行向量相似度检索。
     *
     * <p>先校验知识库处于启用状态，再基于 {@code knowledgeBaseId} 构造 metadata
     * 过滤条件，确保只从目标知识库的向量中召回结果。最后将命中的 Spring AI
     * {@link Document} 转换为包含排序、分数和来源信息的检索结果。</p>
     *
     * @param searchDto 查询文本、知识库 ID、返回数量和相似度阈值
     * @return 包含命中片段及其相似度分数的检索结果
     */
    RetrievalSearchResultDto search(RetrievalSearchDto searchDto);
}
