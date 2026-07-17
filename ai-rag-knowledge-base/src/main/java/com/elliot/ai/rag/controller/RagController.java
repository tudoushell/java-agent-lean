package com.elliot.ai.rag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Slf4j
@RestController
@Tag(name = "RAG 知识库", description = "知识写入与向量相似度检索接口")
public class RagController {
    private final ChatClient chatClient;

    private final VectorStore vectorStore;

    @GetMapping("/test")
    @Operation(summary = "对话测试", description = "将输入直接发送给 ChatClient，并以流式响应返回结果。")
    public Flux<String> test(@RequestParam("input") String input) {
        return chatClient.prompt().user(input).stream().content();
    }


    @GetMapping("/init")
    @Operation(summary = "初始化示例知识", description = "写入两条 Spring 相关示例文档，用于验证向量检索。")
    public void init() {
        List<Document> documents = List.of(
                new Document("spring 是 java 开发框架", Map.of("name", "spring")),
                new Document("springboot 是 java 开发框架", Map.of("name", "springboot"))
        );
        vectorStore.add(documents);
    }

    @GetMapping("/search")
    @Operation(summary = "语义检索", description = "将查询文本转为向量，并返回最相似的三条知识片段。")
    public List<VectorSearchResponse> search(@RequestParam("query") String query) {
        SearchRequest searchRequest = SearchRequest.builder().query(query).topK(3).build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        return documents.stream().map(document -> new VectorSearchResponse(document.getId(), document.getText(), document.getMetadata())).toList();

    }



    public record VectorSearchResponse(String id, String content, Map<String, Object> metadata) {

    }
    

}
