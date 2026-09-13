package com.githubrag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({OpenAiProperties.class, AppProperties.class})
public class OpenAiConfig {

    @Bean
    public WebClient openAiWebClient(OpenAiProperties properties) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://api.openai.com/v1");

        if (properties.hasApiKey()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.getApiKey());
        }

        return builder.build();
    }
}
