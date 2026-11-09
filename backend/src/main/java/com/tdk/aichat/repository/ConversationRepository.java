package com.tdk.aichat.repository;

import com.tdk.aichat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Returns paginated conversations for a user, ordered by updatedAt DESC
     * (Pageable carries the sort).
     */
    Page<Conversation> findByUserId(Long userId, Pageable pageable);

    /**
     * Returns all conversations for a user (used when the full list fits in memory).
     */
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /**
     * Ownership check — returns the conversation only if it belongs to the given user.
     */
    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
