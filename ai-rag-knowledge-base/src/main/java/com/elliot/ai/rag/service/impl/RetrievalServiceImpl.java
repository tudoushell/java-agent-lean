package com.elliot.ai.rag.service.impl;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.dto.RetrievalHitDto;
import com.elliot.ai.rag.dto.RetrievalSearchDto;
import com.elliot.ai.rag.dto.RetrievalSearchResultDto;
import com.elliot.ai.rag.entity.KnowledgeBase;
import com.elliot.ai.rag.enums.KnowledgeBaseStatus;
import com.elliot.ai.rag.service.KnowledgeBaseService;
import com.elliot.ai.rag.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RetrievalServiceImpl implements RetrievalService {

    private final VectorStoreRetriever vectorStoreRetriever;

    private final KnowledgeBaseService knowledgeBaseService;

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
    @Override
    public RetrievalSearchResultDto search(RetrievalSearchDto searchDto) {
        validateKnowledgeBase(searchDto.getKnowledgeBaseId());
        Integer topK = searchDto.getTopK();
        Double threshold = searchDto.getSimilarityThreshold();

        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        Filter.Expression knowledgeBaseFilter = filterBuilder.eq("knowledgeBaseId", searchDto.getKnowledgeBaseId().toString())
                .build();
        SearchRequest searchRequest = SearchRequest.builder()
                .query(searchDto.getQuery().trim())
                .topK(topK)
                .similarityThreshold(threshold)
                .filterExpression(knowledgeBaseFilter)
                .build();

        List<Document> documents = vectorStoreRetriever.similaritySearch(searchRequest);
        List<RetrievalHitDto> results = convertResults(documents);
        return new RetrievalSearchResultDto(searchDto.getKnowledgeBaseId(),
                searchDto.getQuery().trim(),
                topK,
                threshold,
                results.size(), results);
    }

    private List<RetrievalHitDto> convertResults(List<Document> documents) {
        List<RetrievalHitDto> results = new ArrayList<>(documents.size());
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            Map<String, Object> metadata = document.getMetadata();
            UUID chunkId = parseUUid(metadata.get("chunkId"), document.getId());
            UUID documentId = parseUUid(metadata.get("documentId"), null);
            RetrievalHitDto retrievalHitDto = new RetrievalHitDto(
                    index + 1,
                    document.getScore(),
                    chunkId,
                    documentId,
                    getString(metadata, "documentName"),
                    getInteger(metadata, "chunkIndex"),
                    getString(metadata, "sectionTitle"),
                    getInteger(metadata, "pageNumber"),
                    document.getText()
            );
            results.add(retrievalHitDto);
        }
        return results;
    }

    private String getString(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null
                ? null
                : value.toString();
    }

    private Integer getInteger(
            Map<String, Object> metadata,
            String key
    ) {
        Object value = metadata.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private UUID parseUUid(Object metadataValue, String fallbackValue) {
        String value = metadataValue == null ? fallbackValue : metadataValue.toString();
        if (value == null) {
            return null;
        }
        return UUID.fromString(value);
    }


    private void validateKnowledgeBase(UUID knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(knowledgeBaseId);
        if (!KnowledgeBaseStatus.ENABLED.equals(knowledgeBase.getStatus())) {
            throw new BusinessException(ResultCode.FAIL, "知识库已被禁用");
        }
    }
}
