package com.tdk.aichat.service;

import com.tdk.aichat.config.OpenAiConfig;
import com.tdk.aichat.entity.Message;
import com.tdk.aichat.enums.MessageRole;
import com.tdk.aichat.enums.MessageStatus;
import com.tdk.aichat.integration.openai.OpenAiClient;
import com.tdk.aichat.integration.openai.OpenAiRequest;
import com.tdk.aichat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles all LLM-related operations.
 * Provider-specific details (OpenAI) are isolated here so ChatService stays
 * provider-agnostic — swap out OpenAiClient without touching ChatService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {

    private static final String SYSTEM_PROMPT =
            "You are a helpful, knowledgeable, and friendly AI assistant. " +
            "Provide clear, accurate, and concise responses in plain conversational prose. " +
            "Only use markdown formatting (tables, bullet lists, code blocks, headers) " +
            "when the user explicitly asks for it, or when the content is genuinely " +
            "structured (e.g. code, step-by-step instructions, comparisons). " +
            "For general questions, answer naturally in paragraphs — no unnecessary tables or bullet points.";

    private final OpenAiClient openAiClient;
    private final OpenAiConfig openAiConfig;
    private final MessageRepository messageRepository;

    /**
     * Streams the LLM response for the given conversation.
     *
     * @param conversationId the conversation to build context from
     * @param userMessage    the new user message content
     * @param onToken        callback per token
     * @param onComplete     callback on successful completion
     * @param onError        callback on error
     */
    public void streamResponse(
            Long conversationId,
            String userMessage,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError
    ) {
        List<OpenAiRequest.OpenAiMessage> messages = buildContext(conversationId, userMessage);
        OpenAiRequest request = OpenAiRequest.streaming(openAiConfig.getModel(), messages);

        log.debug("Streaming LLM request for conversation={} with {} context messages",
                conversationId, messages.size());

        openAiClient.streamCompletion(request, onToken, onComplete, onError);
    }

    /**
     * Builds the message array for the OpenAI API by retrieving recent conversation
     * history (up to maxContextMessages completed messages) and appending the new prompt.
     */
    @Transactional(readOnly = true)
    public List<OpenAiRequest.OpenAiMessage> buildContext(Long conversationId, String userMessage) {
        List<OpenAiRequest.OpenAiMessage> messages = new ArrayList<>();

        // System message first
        messages.add(new OpenAiRequest.OpenAiMessage("system", SYSTEM_PROMPT));

        // Recent completed messages for context (oldest first)
        int limit = openAiConfig.getMaxContextMessages();
        List<Message> history = messageRepository.findRecentByConversationIdAndStatus(
                conversationId,
                MessageStatus.COMPLETED,
                PageRequest.of(0, limit)
        );

        // Reverse because the query returns newest-first
        for (int i = history.size() - 1; i >= 0; i--) {
            Message m = history.get(i);
            String role = m.getRole() == MessageRole.USER ? "user" : "assistant";
            messages.add(new OpenAiRequest.OpenAiMessage(role, m.getContent()));
        }

        // Current user message
        messages.add(new OpenAiRequest.OpenAiMessage("user", userMessage));

        return messages;
    }
}
