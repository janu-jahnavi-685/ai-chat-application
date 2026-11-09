package com.tdk.aichat.dto.response;

import com.tdk.aichat.entity.Conversation;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ConversationResponse from(Conversation c) {
        return new ConversationResponse(c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
