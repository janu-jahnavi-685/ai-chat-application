package com.tdk.aichat.integration.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenAI Chat Completions API request body.
 */
public record OpenAiRequest(
        String model,
        List<OpenAiMessage> messages,
        boolean stream,
        @JsonProperty("max_tokens") Integer maxTokens,
        Double temperature
) {
    public record OpenAiMessage(String role, String content) {}

    public static OpenAiRequest streaming(String model, List<OpenAiMessage> messages) {
        return new OpenAiRequest(model, messages, true, 2048, 0.7);
    }
}
