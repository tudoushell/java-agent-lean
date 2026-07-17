package com.elliot.ai.rag.test;

import com.elliot.ai.rag.service.KnowledgeBaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class KnowledgeBaseServiceIntegrationTest {
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;


    @Test
    void shouldCreateAndQueryKnowledgeBase() {
                knowledgeBaseService.create(
                        "Java技术知识库",
                        "保存 Java、Spring 和数据库相关文档"
                );
    }
}
