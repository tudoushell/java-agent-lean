package com.elliot.ai.rag.parse;

import com.elliot.ai.rag.config.StorageProperties;
import com.elliot.ai.rag.dto.ParsedText;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public abstract class AbstractDocumentParse {
    private final Path uploadRoot;
    private final Path parseRoot;

    public AbstractDocumentParse(StorageProperties properties) {
        this.uploadRoot = Paths.get(properties.getRootDirectory())
                .toAbsolutePath().normalize();
        this.parseRoot = Paths.get(properties.getParsedDirectory())
                .toAbsolutePath().normalize();
    }

    public abstract boolean isSupport(String extension);

    public ParsedText pares(UUID documentId, String storagePath, String extension) {


        return null;
    }
}
