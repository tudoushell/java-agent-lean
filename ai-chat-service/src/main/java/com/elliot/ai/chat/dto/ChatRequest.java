package com.elliot.ai.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(String id, @NotBlank(message = "消息不能为空") String message, String scene) {

    public ChatRequest {
        if (scene == null || scene.isBlank()) {
            scene = "general";
        }
    }

    public String conversationIdOrDefault() {
        if (id == null || id.isBlank()) {
            return "default-id";
        }
        return id;
    }
}
