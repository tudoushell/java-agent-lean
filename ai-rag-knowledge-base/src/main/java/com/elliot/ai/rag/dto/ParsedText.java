package com.elliot.ai.rag.dto;

public record ParsedText(
        String relativePath,
        String preview,
        long charCount
) {
}