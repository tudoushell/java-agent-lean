package com.elliot.ai.rag.parse;

import com.elliot.ai.rag.config.StorageProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TextDocumentParse extends AbstractDocumentParse {


    private static final Set<String> SUPPORTED_TYPES =
            Set.of("txt", "md");

    public TextDocumentParse(StorageProperties properties) {
        super(properties);
    }

    @Override
    public boolean isSupport(String extension) {
        return SUPPORTED_TYPES.contains(extension);
    }
}
