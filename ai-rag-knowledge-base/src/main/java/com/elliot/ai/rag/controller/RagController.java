package com.elliot.ai.rag.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RequiredArgsConstructor
@Slf4j
@RestController
public class RagController {
    private final ChatClient chatClient;

    @GetMapping("/test")
    public Flux<String> test(@RequestParam("input") String input) {
        return chatClient.prompt().user(input).stream().content();
    }

}
