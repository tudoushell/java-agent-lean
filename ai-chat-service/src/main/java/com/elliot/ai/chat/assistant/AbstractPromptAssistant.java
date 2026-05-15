package com.elliot.ai.chat.assistant;

import com.elliot.ai.chat.config.PromptTemplateConfig;
import com.elliot.ai.chat.dto.PromptTemplate;
import com.elliot.ai.chat.dto.SummaryResponse;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

public abstract class AbstractPromptAssistant implements AiAssistant {

    private static final String QUESTION_PARAM = "question";

    private final ChatClient chatClient;
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

    private ChatClient.ChatClientRequestSpec buildPrompt(String message) {
        PromptTemplate template = promptTemplateConfig.getTemplate(scene);
        return chatClient.prompt()
                .system(template.systemPrompt())
                .user(user -> user.text(template.userTemplate()).param(QUESTION_PARAM, message));
    }
}
