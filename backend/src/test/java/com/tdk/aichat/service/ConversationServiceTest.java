package com.tdk.aichat.service;

import com.tdk.aichat.dto.request.CreateConversationRequest;
import com.tdk.aichat.dto.response.ConversationResponse;
import com.tdk.aichat.entity.Conversation;
import com.tdk.aichat.entity.User;
import com.tdk.aichat.exception.ResourceNotFoundException;
import com.tdk.aichat.repository.ConversationRepository;
import com.tdk.aichat.repository.MessageRepository;
import com.tdk.aichat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ConversationService conversationService;

    private User user;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@example.com").name("User").build();

        conversation = Conversation.builder()
                .id(101L)
                .user(user)
                .title("New Chat")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_shouldReturnConversationResponse() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(conversationRepository.save(any())).thenReturn(conversation);

        ConversationResponse response = conversationService.create(
                "user@example.com", new CreateConversationRequest("New Chat"));

        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.title()).isEqualTo("New Chat");
    }

    @Test
    void listForUser_shouldReturnOnlyUserConversations() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of(conversation));

        List<ConversationResponse> list = conversationService.listForUser("user@example.com");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(101L);
    }

    @Test
    void delete_shouldThrowWhenConversationBelongsToDifferentUser() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(conversationRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.delete("user@example.com", 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_shouldDeleteOwnerConversation() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(conversationRepository.findByIdAndUserId(101L, 1L)).thenReturn(Optional.of(conversation));

        conversationService.delete("user@example.com", 101L);

        verify(conversationRepository).delete(conversation);
    }

    @Test
    void getById_shouldEnforceOwnership() {
        User anotherUser = User.builder().id(2L).email("other@example.com").build();
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(anotherUser));
        when(conversationRepository.findByIdAndUserId(101L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.getById("other@example.com", 101L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
