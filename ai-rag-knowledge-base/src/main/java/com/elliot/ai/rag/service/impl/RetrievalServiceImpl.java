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

/**
 * 向量检索
 */
@RequiredArgsConstructor
@Service
public class RetrievalServiceImpl implements RetrievalService {

    private final VectorStoreRetriever vectorStoreRetriever;

    private final KnowledgeBaseService knowledgeBaseService;


    @Override
    public RetrievalSearchResultDto search(RetrievalSearchDto searchDto) {
        // 仅允许从处于启用状态的知识库中检索。
        validateKnowledgeBase(searchDto.getKnowledgeBaseId());
        Integer topK = searchDto.getTopK();
        Double threshold = searchDto.getSimilarityThreshold();

        // 向量写入时将 knowledgeBaseId 保存为字符串 metadata，因此过滤值也使用字符串。
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        Filter.Expression knowledgeBaseFilter = filterBuilder.eq("knowledgeBaseId", searchDto.getKnowledgeBaseId().toString())
                .build();

        // 查询文本生成向量后，仅在目标知识库的向量范围内按相似度召回 TopK 个结果。
        SearchRequest searchRequest = SearchRequest.builder()
                .query(searchDto.getQuery().trim())
                .topK(topK)
                .similarityThreshold(threshold)
                .filterExpression(knowledgeBaseFilter)
                .build();

        // 将向量库返回的 Document 及 metadata 转换为接口需要的命中结果。
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
