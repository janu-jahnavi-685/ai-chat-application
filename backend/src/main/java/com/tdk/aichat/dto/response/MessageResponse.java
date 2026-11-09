package com.tdk.aichat.dto.response;

import com.tdk.aichat.entity.Message;
import com.tdk.aichat.enums.MessageRole;
import com.tdk.aichat.enums.MessageStatus;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        MessageRole role,
        String content,
        MessageStatus status,
        LocalDateTime createdAt
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getRole(), m.getContent(), m.getStatus(), m.getCreatedAt());
    }
}
