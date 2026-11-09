package com.tdk.aichat.controller;

import com.tdk.aichat.dto.request.ChatRequest;
import com.tdk.aichat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "LLM chat with SSE streaming")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L;

    private final ChatService chatService;

    @Qualifier("sseTaskExecutor")
    private final Executor sseTaskExecutor;

    /**
     * POST /api/chat/{conversationId}/stream
     *
     * Returns text/event-stream. Uses fetch() on the frontend (not native EventSource)
     * because we need Authorization header + POST body.
     *
     * The streaming work is submitted to sseTaskExecutor so it runs off the
     * Tomcat request thread — @Async cannot be used here because Spring's async
     * proxy rejects SseEmitter return types (only Future/void are supported).
     */
    @PostMapping(
            value = "/{conversationId}/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @Operation(summary = "Send a message and stream the AI response")
    public SseEmitter stream(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long conversationId,
            @Valid @RequestBody ChatRequest request
    ) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        log.debug("SSE stream started: conversation={}, user={}",
                conversationId, userDetails.getUsername());

        // Submit streaming work to the dedicated executor — keeps Tomcat threads free
        sseTaskExecutor.execute(() ->
                chatService.handleStreamingChat(
                        userDetails.getUsername(),
                        conversationId,
                        request.message(),
                        emitter
                )
        );

        return emitter;
    }
}
