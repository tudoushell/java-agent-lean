package com.elliot.ai.chat.assistant;

import com.elliot.ai.chat.config.PromptTemplateConfig;
import com.elliot.ai.chat.dto.PromptTemplate;
import com.elliot.ai.chat.dto.SummaryResponse;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

public abstract class AbstractPromptAssistant implements AiAssistant {

    private static final String QUESTION_PARAM = "question";

    protected final ChatClient chatClient;
    private final PromptTemplateConfig promptTemplateConfig;
    @Getter
    private final String scene;

    protected AbstractPromptAssistant(ChatClient chatClient,
                                      PromptTemplateConfig promptTemplateConfig,
                                      String scene) {
        this.chatClient = chatClient;
        this.promptTemplateConfig = promptTemplateConfig;
        this.scene = scene;
    }

    @Override
    public String chat(String message) {
        String content = buildPrompt(message)
                .call()
                .content();
        return content == null ? "" : content;
    }

    @Override
    public Flux<String> chatStream(String message) {
        return buildPrompt(message)
                .stream()
                .content();
    }

    @Override
    public SummaryResponse summary(String message) {
        return buildPrompt(message).call().entity(SummaryResponse.class);
    }

    @Override
    public Flux<String> chatStreamWithMemory(String id, String message) {
        return buildPromptWithMemory(message).advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, id))
                .stream().content();
    }

    @Override
    public Flux<String> chatStreamWithTool(String id, String message) {
        return null;
    }

    protected PromptTemplate getPromptTemplate() {
        return promptTemplateConfig.getTemplate(scene);
    }

    protected ChatClient.ChatClientRequestSpec buildPrompt(String message) {
        PromptTemplate template = getPromptTemplate();
        return chatClient.prompt()
                .system(template.systemPrompt())
                .user(user -> user.text(template.userTemplate()).param(QUESTION_PARAM, message));
    }

    protected ChatClient.ChatClientRequestSpec buildPromptWithMemory(String message) {
        PromptTemplate template = getPromptTemplate();
        return chatClient.prompt()
                .system(template.systemPrompt())
                .user(message);
    }
}
