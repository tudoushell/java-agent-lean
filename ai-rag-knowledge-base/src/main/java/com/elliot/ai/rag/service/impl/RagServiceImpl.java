package com.elliot.ai.rag.service.impl;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.common.prompt.PromptTemplate;
import com.elliot.ai.common.prompt.PromptTemplateConfig;
import com.elliot.ai.rag.config.RagProperties;
import com.elliot.ai.rag.dto.RagChatDto;
import com.elliot.ai.rag.dto.RagChatResultDto;
import com.elliot.ai.rag.dto.RagSourceDto;
import com.elliot.ai.rag.dto.RetrievalHitDto;
import com.elliot.ai.rag.dto.RetrievalSearchDto;
import com.elliot.ai.rag.dto.RetrievalSearchResultDto;
import com.elliot.ai.rag.dto.TokenUsageDto;
import com.elliot.ai.rag.service.RagService;
import com.elliot.ai.rag.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RagServiceImpl implements RagService {

    private final RetrievalService retrievalService;
    private final ChatClient chatClient;
    private final PromptTemplateConfig promptTemplateConfig;
    private final RagProperties ragProperties;

    @Override
    public RagChatResultDto ragChat(RagChatDto ragChatDto) {
        String question = ragChatDto.question().trim();
        int topK = ragChatDto.topK() == null ? ragProperties.getTopK() : ragChatDto.topK();
        double threshold = ragChatDto.similarityThreshold() == null ? ragProperties.getSimilarityThreshold() : ragChatDto.similarityThreshold();
        RetrievalSearchResultDto retrieval = retrievalService.search(new RetrievalSearchDto(
                ragChatDto.knowledgeBaseId(),
                question,
                topK,
                threshold
        ));
        if (retrieval.hits().isEmpty()) {
            return new RagChatResultDto(
                    ragChatDto.knowledgeBaseId(),
                    question,
                    "NOT ANSWER",
                    false,
                    List.of(),
                    null);
        }
        ContextResult contextResult = buildContext(retrieval.hits());
        PromptTemplate ragTemplate = promptTemplateConfig.getTemplate("rag");
        ChatResponse chatResponse = chatClient.prompt()
                .system(ragTemplate.systemPrompt())
                .user(user -> user.text(ragTemplate.userTemplate())
                        .param("context", contextResult.content())
                        .param("question", question))
                .call()
                .chatResponse();
        String answer = extractAnswer(chatResponse);
        return new RagChatResultDto(
                ragChatDto.knowledgeBaseId(),
                question,
                answer,
                true,
                contextResult.ragSources(),
                extractUsage(chatResponse)
        );
    }

    private TokenUsageDto extractUsage(ChatResponse chatResponse) {
        if (chatResponse == null
                || chatResponse.getMetadata() == null) {
            return null;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return null;
        }
        return new TokenUsageDto(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    private String extractAnswer(ChatResponse chatResponse) {
        if (chatResponse == null
                || chatResponse.getResult() == null
                || chatResponse
                .getResult()
                .getOutput() == null) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "大模型没有返回有效回答"
            );
        }

        String answer = chatResponse
                .getResult()
                .getOutput()
                .getText();

        if (!StringUtils.hasText(answer)) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "大模型没有返回有效回答"
            );
        }
        return answer.trim();
    }

    private ContextResult buildContext(List<RetrievalHitDto> hits) {
        StringBuilder contextBuilder = new StringBuilder();
        List<RagSourceDto> sources = new ArrayList<>();
        int maxChars = ragProperties.getMaxContextChars();
        for (int index = 0; index < hits.size(); index++) {
            RetrievalHitDto hit = hits.get(index);
            String referenceId = "S" + (index + 1);
            String contextItem = buildContextItem(referenceId, hit);
            /*
             * 至少保留第一条资料。
             * 后面的资料超过 Context 限制则停止。
             */
            if (!contextBuilder.isEmpty()
                    && contextBuilder.length()
                    + contextItem.length()
                    > maxChars) {
                break;
            }
            contextBuilder.append(contextItem);
            sources.add(new RagSourceDto(
                    referenceId,
                    hit.rank(),
                    hit.score(),
                    hit.chunkId(),
                    hit.documentId(),
                    hit.documentName(),
                    hit.chunkIndex(),
                    hit.sectionTitle(),
                    hit.pageNumber(),
                    hit.content()
            ));
        }
        return new ContextResult(contextBuilder.toString(), sources);
    }

    private String buildContextItem(String referenceId, RetrievalHitDto hit) {
        StringBuilder builder = new StringBuilder();

        builder.append("[")
                .append(referenceId)
                .append("]\n");

        builder.append("文档：")
                .append(
                        hit.documentName()
                )
                .append("\n");

        if (StringUtils.hasText(hit.sectionTitle())) {
            builder.append("章节：")
                    .append(hit.sectionTitle())
                    .append("\n");
        }

        if (hit.pageNumber() != null) {
            builder.append("页码：")
                    .append(hit.pageNumber())
                    .append("\n");
        }

        builder.append("内容：\n")
                .append(hit.content())
                .append("\n\n");

        return builder.toString();
    }

    public record ContextResult(
            String content,
            List<RagSourceDto> ragSources
    ) {

    }
}
