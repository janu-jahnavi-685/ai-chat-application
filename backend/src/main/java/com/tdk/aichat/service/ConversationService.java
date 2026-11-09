package com.tdk.aichat.service;

import com.tdk.aichat.dto.request.CreateConversationRequest;
import com.tdk.aichat.dto.response.ConversationResponse;
import com.tdk.aichat.dto.response.MessageResponse;
import com.tdk.aichat.entity.Conversation;
import com.tdk.aichat.entity.User;
import com.tdk.aichat.exception.ResourceNotFoundException;
import com.tdk.aichat.repository.ConversationRepository;
import com.tdk.aichat.repository.MessageRepository;
import com.tdk.aichat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ConversationResponse create(String userEmail, CreateConversationRequest request) {
        User user = getUser(userEmail);

        Conversation conversation = Conversation.builder()
                .user(user)
                .title(request.title())
                .build();

        Conversation saved = conversationRepository.save(conversation);
        log.debug("Created conversation id={} for user id={}", saved.getId(), user.getId());
        return ConversationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listForUser(String userEmail) {
        User user = getUser(userEmail);
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(ConversationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getById(String userEmail, Long conversationId) {
        User user = getUser(userEmail);
        Conversation conversation = getOwnedConversation(conversationId, user.getId());
        return ConversationResponse.from(conversation);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(String userEmail, Long conversationId) {
        User user = getUser(userEmail);
        // Verify ownership
        getOwnedConversation(conversationId, user.getId());

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public void delete(String userEmail, Long conversationId) {
        User user = getUser(userEmail);
        Conversation conversation = getOwnedConversation(conversationId, user.getId());
        conversationRepository.delete(conversation);
        log.debug("Deleted conversation id={} for user id={}", conversationId, user.getId());
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Conversation getOwnedConversation(Long conversationId, Long userId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation not found: " + conversationId));
    }
}
