-- V3: Create messages table
CREATE TABLE messages (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT      NOT NULL,
    role            VARCHAR(20) NOT NULL,
    content         TEXT        NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for loading conversation messages in chronological order
CREATE INDEX idx_messages_conversation_created ON messages (conversation_id, created_at);
