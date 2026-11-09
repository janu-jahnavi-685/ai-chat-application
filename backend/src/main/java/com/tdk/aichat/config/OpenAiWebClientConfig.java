package com.tdk.aichat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Builds the LLM WebClient using @Value — resolved AFTER all properties are bound,
 * avoiding the @ConfigurationProperties early-init ordering issue.
 */
@Configuration
@Slf4j
public class OpenAiWebClientConfig {

    @Value("${openai.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Bean(name = "openAiWebClient")
    public WebClient openAiWebClient() {
        log.info("LLM WebClient initialised → baseUrl={}", baseUrl);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}
