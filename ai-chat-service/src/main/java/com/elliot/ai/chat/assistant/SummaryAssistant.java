package com.elliot.ai.chat.assistant;

import com.elliot.ai.common.prompt.PromptTemplateConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SummaryAssistant extends AbstractPromptAssistant {

    private static final String SCENE = "summary";

    public SummaryAssistant(@Qualifier("qwen3") ChatClient chatClient,
                            PromptTemplateConfig promptTemplateConfig) {
        super(chatClient, promptTemplateConfig, SCENE);
    }
}
