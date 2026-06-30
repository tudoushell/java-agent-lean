package com.elliot.ai.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    public ChatClient chatClient() {
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder().openAiApi(OpenAiApi.builder()
                        .baseUrl("http://localhost:11434")
                        .apiKey("ollama")
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen3.5:0.8b")
                        .build()).build();
        return ChatClient.create(openAiChatModel);
    }



}
