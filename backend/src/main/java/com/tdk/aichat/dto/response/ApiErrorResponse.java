package com.tdk.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        int status,
        String error,
        String message
) {
    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(LocalDateTime.now(), status, error, message);
    }
}
