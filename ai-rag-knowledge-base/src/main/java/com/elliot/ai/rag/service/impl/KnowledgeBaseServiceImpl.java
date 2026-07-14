package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elliot.ai.rag.entity.KnowledgeBase;
import com.elliot.ai.rag.enums.KnowledgeBaseStatus;
import com.elliot.ai.rag.mapper.KnowledgeBaseMapper;
import com.elliot.ai.rag.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {

    @Transactional(rollbackFor = Exception.class)
    public void create(String name, String description) {
        String normalizedName = normalizeName(name);
        String normalizedDescription = normalizeDescription(description);

        OffsetDateTime now = OffsetDateTime.now();

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(UUID.randomUUID());
        knowledgeBase.setName(normalizedName);
        knowledgeBase.setDescription(normalizedDescription);
        knowledgeBase.setStatus(
                KnowledgeBaseStatus.ENABLED
        );
        knowledgeBase.setCreatedAt(now);
        knowledgeBase.setUpdatedAt(now);
        this.save(knowledgeBase);
    }


    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException(
                    "知识库名称不能为空"
            );
        }

        String normalized = name.trim();

        if (normalized.length() > 128) {
            throw new IllegalArgumentException(
                    "知识库名称不能超过 128 个字符"
            );
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }

        String normalized = description.trim();

        if (normalized.length() > 500) {
            throw new IllegalArgumentException(
                    "知识库描述不能超过 500 个字符"
            );
        }

        return normalized;
    }
}
