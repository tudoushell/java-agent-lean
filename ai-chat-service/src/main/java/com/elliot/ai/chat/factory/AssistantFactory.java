package com.elliot.ai.chat.factory;

import com.elliot.ai.chat.assistant.AbstractPromptAssistant;
import com.elliot.ai.chat.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AssistantFactory {

    private static final String DEFAULT_SCENE = "general";

    private final Map<String, AbstractPromptAssistant> assistantMap;

    public AssistantFactory(List<AbstractPromptAssistant> assistants) {
        this.assistantMap = assistants.stream()
                .collect(Collectors.toUnmodifiableMap(AbstractPromptAssistant::getScene, Function.identity()));
    }

    public AbstractPromptAssistant getAssistant(String scene) {
        String resolvedScene = resolveScene(scene);
        AbstractPromptAssistant assistant = assistantMap.get(resolvedScene);
        if (assistant == null) {
            throw new BusinessException("ASSISTANT_NOT_FOUND", "Assistant 不存在：" + resolvedScene);
        }
        return assistant;
    }

    private String resolveScene(String scene) {
        if (scene == null || scene.isBlank()) {
            return DEFAULT_SCENE;
        }
        return scene;
    }
}
