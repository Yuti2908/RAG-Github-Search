package com.githubrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GithubRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubRagApplication.class, args);
    }
}
