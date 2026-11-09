package com.tdk.aichat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(

        @NotBlank(message = "Message cannot be empty")
        @Size(max = 10000, message = "Message must not exceed 10000 characters")
        String message
) {}
