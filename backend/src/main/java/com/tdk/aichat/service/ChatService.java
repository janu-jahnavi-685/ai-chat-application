package com.tdk.aichat.service;

import com.tdk.aichat.entity.Conversation;
import com.tdk.aichat.entity.Message;
import com.tdk.aichat.entity.User;
import com.tdk.aichat.enums.MessageRole;
import com.tdk.aichat.enums.MessageStatus;
import com.tdk.aichat.exception.ResourceNotFoundException;
import com.tdk.aichat.repository.ConversationRepository;
import com.tdk.aichat.repository.MessageRepository;
import com.tdk.aichat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates the chat flow:
 * 1. Validate user and conversation ownership
 * 2. Persist the user message
 * 3. Auto-generate conversation title on first message
 * 4. Create assistant message placeholder (status=FAILED, updated on success)
 * 5. Send thinking status SSE event
 * 6. Stream LLM response via LlmService
 * 7. Persist complete assistant response with status=COMPLETED
 * 8. Send done SSE event
 * 9. Handle errors and disconnections cleanly
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final int MAX_TITLE_LENGTH = 60;

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final LlmService llmService;

    /**
     * Entry point called by ChatController.
     * Runs on the sseTaskExecutor thread pool (not Tomcat's request thread).
     */
    public void handleStreamingChat(
            String userEmail,
            Long conversationId,
            String messageContent,
            SseEmitter emitter
    ) {
        AtomicBoolean cancelled = new AtomicBoolean(false);

        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> cancelled.set(true));
        emitter.onError(t -> cancelled.set(true));

        try {
            // ── 1. Validate ownership ─────────────────────────────────────────
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Conversation conversation = conversationRepository
                    .findByIdAndUserId(conversationId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Conversation not found: " + conversationId));

            // ── 2. Persist user message ───────────────────────────────────────
            boolean isFirstMessage = messageRepository.countByConversationId(conversationId) == 0;

            Message userMessage = messageRepository.save(Message.builder()
                    .conversation(conversation)
                    .role(MessageRole.USER)
                    .content(messageContent)
                    .status(MessageStatus.COMPLETED)
                    .build());

            // ── 3. Auto-generate title from first message ─────────────────────
            if (isFirstMessage) {
                String title = messageContent.length() > MAX_TITLE_LENGTH
                        ? messageContent.substring(0, MAX_TITLE_LENGTH - 3) + "..."
                        : messageContent;
                conversation.setTitle(title);
                conversationRepository.save(conversation);
            }

            // ── 4. Create assistant placeholder (FAILED → updated to COMPLETED) ─
            Message assistantMessage = messageRepository.save(Message.builder()
                    .conversation(conversation)
                    .role(MessageRole.ASSISTANT)
                    .content("")
                    .status(MessageStatus.FAILED)
                    .build());

            // ── 5. Send thinking status ───────────────────────────────────────
            sendEvent(emitter, "status",
                    Map.of("status", "THINKING", "message", "Analyzing your question..."),
                    cancelled);

            // ── 6. Stream from LLM ────────────────────────────────────────────
            StringBuilder accumulated = new StringBuilder();

            llmService.streamResponse(
                    conversationId,
                    messageContent,
                    // onToken
                    token -> {
                        if (!cancelled.get()) {
                            accumulated.append(token);
                            sendEvent(emitter, "token", Map.of("content", token), cancelled);
                        }
                    },
                    // onComplete
                    () -> {
                        if (!cancelled.get()) {
                            try {
                                // ── 7. Persist complete response ──────────────
                                String fullResponse = accumulated.toString();
                                assistantMessage.setContent(fullResponse);
                                assistantMessage.setStatus(MessageStatus.COMPLETED);
                                messageRepository.save(assistantMessage);

                                // Update conversation timestamp
                                conversation.setUpdatedAt(LocalDateTime.now());
                                conversationRepository.save(conversation);

                                // ── 8. Send done event ────────────────────────
                                sendEvent(emitter, "done",
                                        Map.of("messageId", assistantMessage.getId(),
                                               "status", "COMPLETED"),
                                        cancelled);
                                emitter.complete();
                            } catch (Exception e) {
                                log.error("Error finalizing chat for conversation={}", conversationId, e);
                                emitter.completeWithError(e);
                            }
                        }
                    },
                    // onError
                    error -> {
                        log.error("LLM error for conversation={}: {}", conversationId, error.getMessage());
                        // Keep FAILED status on assistant message (already persisted as FAILED)
                        if (!cancelled.get()) {
                            sendEvent(emitter, "error",
                                    Map.of("code", "LLM_ERROR",
                                           "message", "Unable to generate response. Please try again."),
                                    cancelled);
                            emitter.complete();
                        }
                    }
            );

        } catch (Exception e) {
            log.error("ChatService error for conversation={}: {}", conversationId, e.getMessage());
            if (!cancelled.get()) {
                sendEvent(emitter, "error",
                        Map.of("code", "SERVER_ERROR", "message", "An error occurred. Please try again."),
                        cancelled);
                emitter.complete();
            }
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data, AtomicBoolean cancelled) {
        if (cancelled.get()) return;
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.debug("SSE send failed (client disconnected): {}", e.getMessage());
            cancelled.set(true);
        }
    }
}
