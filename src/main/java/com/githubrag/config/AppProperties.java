package com.githubrag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.github")
public class AppProperties {

    /** Falls back to this username when a request doesn't specify one. */
    private String defaultUsername;
}
