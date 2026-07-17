package com.elliot.ai.chat.config;

import com.elliot.ai.chat.dto.PromptTemplate;
import com.elliot.ai.chat.exception.BusinessException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class PromptTemplateConfig {

    private final Map<String, PromptTemplate> cache = new HashMap<>();

    public PromptTemplate getTemplate(String sceneCode) {
        String realScene = resolveScene(sceneCode);
        return cache.computeIfAbsent(realScene, this::loadPromptTemplate);
    }


    private String resolveScene(String sceneCode) {
        if (sceneCode == null || sceneCode.isBlank()) {
            return "general";
        }
        return sceneCode;
    }

    private PromptTemplate loadPromptTemplate(String scene) {
        String systemPrompt = readPromptTemplate(scene, "system.md");
        String userTemplate = readPromptTemplate(scene, "user.md");
        return new PromptTemplate(scene, systemPrompt, userTemplate);
    }

    private String readPromptTemplate(String scene, String fileName) {
        String path = "prompts/" + scene + "/" + fileName;

        ClassPathResource resource = new ClassPathResource(path);

        if (!resource.exists()) {
            throw new BusinessException(
                    "PROMPT_NOT_FOUND",
                    "Prompt 文件不存在：" + path
            );
        }

        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(
                    "PROMPT_READ_ERROR",
                    "读取 Prompt 文件失败：" + path
            );
        }
    }
}
