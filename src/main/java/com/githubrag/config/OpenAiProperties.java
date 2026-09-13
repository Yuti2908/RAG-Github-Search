package com.githubrag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.openai")
public class OpenAiProperties {

    private String apiKey;
    private String embeddingModel;
    private String chatModel;
    private int embeddingDimension;

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
