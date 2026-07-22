package com.elliot.ai.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rag.retrieval")
@Setter
@Getter
public class RagProperties {

    private int topK = 5;

    private double similarityThreshold = 0.5;

    private int maxContextChars = 20000;

}
