package com.tdk.aichat.integration.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Parses a single SSE data line from the OpenAI streaming response.
 *
 * OpenAI sends:  data: {"id":"...","choices":[{"delta":{"content":"token"},...}]}
 * Termination:   data: [DONE]
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiStreamHandler {

    private static final String DONE_SIGNAL = "[DONE]";
    private final ObjectMapper objectMapper;

    /**
     * Parses a raw SSE data line and extracts the content token.
     *
     * @param dataLine the raw value after "data: " prefix
     * @return Optional.empty() if DONE or no content; Optional.of(token) otherwise
     */
    public Optional<String> extractToken(String dataLine) {
        if (dataLine == null || dataLine.isBlank()) {
            return Optional.empty();
        }

        String trimmed = dataLine.trim();
        if (DONE_SIGNAL.equals(trimmed)) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(trimmed);
            log.debug("Stream chunk: {}", trimmed); // ← temporary debug

            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                return Optional.empty();
            }

            JsonNode delta = choices.get(0).path("delta");
            JsonNode content = delta.path("content");

            if (content.isMissingNode() || content.isNull()) {
                return Optional.empty();
            }
            String text = content.asText();
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (Exception e) {
            log.debug("Failed to parse stream chunk: {} — error: {}", dataLine, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean isDone(String dataLine) {
        return dataLine != null && DONE_SIGNAL.equals(dataLine.trim());
    }
}
