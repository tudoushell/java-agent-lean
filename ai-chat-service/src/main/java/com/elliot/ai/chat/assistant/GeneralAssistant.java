package com.elliot.ai.chat.assistant;

import com.elliot.ai.common.prompt.PromptTemplateConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class GeneralAssistant extends AbstractPromptAssistant {

    private static final String SCENE = "general";

    public GeneralAssistant(@Qualifier("qwen3") ChatClient chatClient,
                            PromptTemplateConfig promptTemplateConfig) {
        super(chatClient, promptTemplateConfig, SCENE);
    }
}
