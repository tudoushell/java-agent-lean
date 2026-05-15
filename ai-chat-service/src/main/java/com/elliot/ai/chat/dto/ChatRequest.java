package com.elliot.ai.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank(message = "消息不能为空") String message, String scene) {

    public ChatRequest {
        if (scene == null || scene.isBlank()) {
            scene = "general";
        }
    }
}
