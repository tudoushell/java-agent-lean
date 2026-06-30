package com.elliot.ai.rag.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("http://localhost:11434")
                .apiKey("ollama")
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model("nomic-embed-text")
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }
}
