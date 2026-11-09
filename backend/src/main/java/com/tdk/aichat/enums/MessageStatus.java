package com.tdk.aichat.enums;

/**
 * Lifecycle status of a message.
 * FAILED is also used as a transient "pending" state for assistant
 * messages created before streaming begins — updated to COMPLETED on success.
 */
public enum MessageStatus {
    COMPLETED,
    FAILED
}
