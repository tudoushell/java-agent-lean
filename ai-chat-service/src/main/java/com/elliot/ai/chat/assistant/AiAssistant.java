package com.elliot.ai.chat.assistant;

import com.elliot.ai.chat.dto.SummaryResponse;
import reactor.core.publisher.Flux;

public interface AiAssistant {

    String chat(String message);

    Flux<String> chatStream(String message);

    SummaryResponse summary(String message);

    Flux<String> chatStreamWithMemory(String id, String message);

    Flux<String> chatStreamWithTool(String id, String message);
}
