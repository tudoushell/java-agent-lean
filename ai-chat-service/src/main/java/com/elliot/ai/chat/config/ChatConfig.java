package com.elliot.ai.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class ChatConfig {

    @Bean(name = "qwen3")
    public ChatClient chatClient(ChatMemory chatMemory) {
        OpenAiChatModel openChatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl("http://localhost:11434")
                        .apiKey("ollama")
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen3-vl:2b")
                        .build()).build();
        return ChatClient.builder(openChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean(name = "qwen4")
    public ChatClient qwen4() {
        OpenAiChatModel openChatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl("http://localhost:11434")
                        .apiKey("ollama")
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen3-vl:2b")
                        .build()).build();
        return ChatClient.builder(openChatModel).defaultSystem("所有回答使用简洁的中文回答").build();
    }



    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }


}
