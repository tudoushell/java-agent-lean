package com.elliot.ai.chat.controller;

import com.elliot.ai.chat.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final ChatClient chatClient;

    public AiController(@Qualifier("qwen4") ChatClient chatClient) {
        this.chatClient = chatClient;
    }


    @GetMapping("/chat")
    public String chat(ChatRequest request) {
        return chatClient.prompt()
                .user(request.message())
                .call()
                .content();
    }
}
