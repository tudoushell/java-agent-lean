package com.elliot.ai.chat.assistant;

import com.elliot.ai.chat.config.PromptTemplateConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class InterviewerAssistant extends AbstractPromptAssistant {

    private static final String SCENE = "interviewer";

    public InterviewerAssistant(@Qualifier("qwen3") ChatClient chatClient,
                                PromptTemplateConfig promptTemplateConfig) {
        super(chatClient, promptTemplateConfig, SCENE);
    }
}
