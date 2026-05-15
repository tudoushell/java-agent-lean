package com.elliot.ai.chat.dto;

public record PromptTemplate(
        String scene,
        String systemPrompt,
        String userTemplate
) {
}
