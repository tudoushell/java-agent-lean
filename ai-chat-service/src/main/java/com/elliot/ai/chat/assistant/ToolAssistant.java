package com.elliot.ai.chat.assistant;

import com.elliot.ai.chat.config.PromptTemplateConfig;
import com.elliot.ai.chat.dto.PromptTemplate;
import com.elliot.ai.chat.tool.OrderTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ToolAssistant extends AbstractPromptAssistant {

    private static final String SCENE = "tool_assistant";

    @Autowired
    private OrderTools orderTools;

    protected ToolAssistant(@Qualifier("qwen3") ChatClient chatClient,
                            PromptTemplateConfig promptTemplateConfig) {
        super(chatClient, promptTemplateConfig, SCENE);
    }

    @Override
    public Flux<String> chatStreamWithTool(String id, String message) {
        PromptTemplate promptTemplate = getPromptTemplate();
        return chatClient.prompt().system(promptTemplate.systemPrompt())
                .tools(orderTools)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, id))
                .user(message)
                .stream()
                .content();
    }
}
