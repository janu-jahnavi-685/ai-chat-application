package com.tdk.aichat.integration.openai;

import com.tdk.aichat.config.OpenAiConfig;
import com.tdk.aichat.exception.LlmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * Low-level WebClient wrapper for the OpenAI Chat Completions API.
 *
 * Uses streaming mode (stream: true) and returns a Flux<String> of raw SSE data lines.
 * The caller (LlmService) is responsible for subscribing, parsing tokens, and
 * accumulating the response.
 */
@Component
@Slf4j
public class OpenAiClient {

    private final WebClient webClient;
    private final OpenAiConfig config;
    private final OpenAiStreamHandler streamHandler;

    public OpenAiClient(
            @Qualifier("openAiWebClient") WebClient webClient,
            OpenAiConfig config,
            OpenAiStreamHandler streamHandler
    ) {
        this.webClient = webClient;
        this.config = config;
        this.streamHandler = streamHandler;
    }

    /**
     * Streams tokens from OpenAI.
     *
     * @param request         the fully-constructed request
     * @param onToken         callback for each extracted text token
     * @param onComplete      callback when stream finishes normally
     * @param onError         callback on error
     */
    public void streamCompletion(
            OpenAiRequest request,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError
    ) {
        webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.bodyToMono(String.class)
                        .map(body -> {
                            log.error("Groq/OpenAI 4xx error — status={} body={}", response.statusCode().value(), body);
                            if (response.statusCode().value() == 401) {
                                return new LlmException("Invalid API key — check OPENAI_API_KEY in .env");
                            } else if (response.statusCode().value() == 404) {
                                return new LlmException("LLM endpoint not found (404) — check OPENAI_BASE_URL. Got: " + body);
                            } else if (response.statusCode().value() == 429) {
                                return new LlmException("Rate limit exceeded. Please try again later.");
                            }
                            return new LlmException("LLM client error " + response.statusCode().value() + ": " + body);
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .map(body -> {
                                    log.error("Groq/OpenAI 5xx error — status={} body={}", response.statusCode().value(), body);
                                    return new LlmException("LLM server error " + response.statusCode().value() + ": " + body);
                                }))
                .bodyToFlux(String.class)
                .timeout(config.getTimeout())
                .subscribe(
                        line -> {
                            if (line == null || line.isBlank()) return;
                            // Handle both "data: {...}" and raw "{...}" formats
                            String data = line.startsWith("data: ")
                                    ? line.substring(6).trim()
                                    : line.trim();
                            if (!data.isEmpty() && !data.equals("[DONE]")) {
                                streamHandler.extractToken(data).ifPresent(onToken);
                            }
                        },
                        error -> {
                            log.error("OpenAI streaming error: {}", error.getMessage());
                            if (error instanceof WebClientResponseException wex) {
                                onError.accept(new LlmException("OpenAI error: " + wex.getStatusCode()));
                            } else {
                                onError.accept(new LlmException("LLM streaming failed", error));
                            }
                        },
                        onComplete
                );
    }
}
