package com.tdk.aichat.repository;

import com.tdk.aichat.entity.Message;
import com.tdk.aichat.enums.MessageRole;
import com.tdk.aichat.enums.MessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Returns all messages in a conversation in chronological order.
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Returns the most recent N completed messages for LLM context building.
     * Uses a sub-query to get the last N records in ascending order for the API.
     */
    @Query("""
            SELECT m FROM Message m
            WHERE m.conversation.id = :conversationId
              AND m.status = :status
            ORDER BY m.createdAt DESC
            """)
    List<Message> findRecentByConversationIdAndStatus(
            @Param("conversationId") Long conversationId,
            @Param("status") MessageStatus status,
            Pageable pageable);

    /**
     * Count of messages in a conversation (to detect the first message for title generation).
     */
    long countByConversationId(Long conversationId);

    /**
     * Count of completed messages by role in a conversation.
     */
    long countByConversationIdAndRoleAndStatus(Long conversationId, MessageRole role, MessageStatus status);
}
