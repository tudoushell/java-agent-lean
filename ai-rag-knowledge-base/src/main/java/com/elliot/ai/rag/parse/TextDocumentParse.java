package com.elliot.ai.rag.parse;

import com.elliot.ai.rag.config.StorageProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Override
    protected String parseFile(Path sourcePath) throws IOException {
        String content = Files.readString(sourcePath, StandardCharsets.UTF_8);
        return normalize(content);
    }
}
