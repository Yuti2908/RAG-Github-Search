package com.githubrag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GitHubApiConfig {

    @Bean
    public WebClient githubApiWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Bean
    public WebClient githubRawWebClient() {
        return WebClient.builder()
                .baseUrl("https://raw.githubusercontent.com")
                .build();
    }
}
