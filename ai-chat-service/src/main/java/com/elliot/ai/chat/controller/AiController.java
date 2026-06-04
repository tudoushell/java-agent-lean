package com.elliot.ai.chat.controller;

import com.elliot.ai.chat.dto.ApiResponse;
import com.elliot.ai.chat.dto.ChatRequest;
import com.elliot.ai.chat.dto.SummaryResponse;
import com.elliot.ai.chat.factory.AssistantFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.Disposable;

@RestController
public class AiController {

    private final AssistantFactory assistantFactory;

    public AiController(AssistantFactory assistantFactory) {
        this.assistantFactory = assistantFactory;
    }

    @GetMapping("/chat")
    public ApiResponse<String> chat(ChatRequest request) {
        return ApiResponse.success(assistantFactory.getAssistant(request.scene()).chat(request.message()));
    }

    @GetMapping(value = "/chat2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreaming(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        Flux<String> content = assistantFactory.getAssistant(request.scene()).chatStream(request.message());
        Disposable disposable = content.subscribe(
                chunk -> {
                    try {
                        emitter.send(chunk);
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError,
                emitter::complete
        );

        emitter.onCompletion(disposable::dispose);
        emitter.onTimeout(disposable::dispose);
        emitter.onError(error -> disposable.dispose());
        return emitter;
    }


    @GetMapping(value = "/chat3")
    public Flux<String> chatStreamingFlux(ChatRequest request) {
        return assistantFactory.getAssistant(request.scene()).chatStream(request.message());
    }

    @GetMapping(value = "/chat4")
    public SummaryResponse summary(ChatRequest request) {
        return assistantFactory.getAssistant("summary").summary(request.message());
    }

    /**
     * memory 记忆
     *
     * @param request
     * @return
     */
    @GetMapping(value = "/mem-chat")
    public Flux<String> chatStreamWithMemory(ChatRequest request) {
        return assistantFactory.getAssistant(request.scene()).chatStreamWithMemory(request.conversationIdOrDefault(), request.message());
    }

    @GetMapping(value = "/tool-chat")
    public Flux<String> chatStreamWithTool(ChatRequest request) {
        return assistantFactory.getAssistant("tool_assistant").chatStreamWithTool(request.conversationIdOrDefault(), request.message());
    }
}
