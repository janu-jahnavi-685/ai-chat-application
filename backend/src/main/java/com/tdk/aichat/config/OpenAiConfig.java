package com.tdk.aichat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Holds all OpenAI / LLM provider configuration properties.
 * WebClient bean is in OpenAiWebClientConfig — separate to avoid early-init issues.
 */
@Component
@ConfigurationProperties(prefix = "openai")
@Getter
@Setter
public class OpenAiConfig {

    private String apiKey;
    private String model;
    private String baseUrl;
    private long timeoutSeconds;
    private int maxContextMessages;

    public Duration getTimeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }
}
