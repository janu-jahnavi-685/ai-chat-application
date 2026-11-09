package com.tdk.aichat.controller;

import com.tdk.aichat.dto.request.CreateConversationRequest;
import com.tdk.aichat.dto.response.ConversationResponse;
import com.tdk.aichat.dto.response.MessageResponse;
import com.tdk.aichat.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Conversation management")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @Operation(summary = "Create a new conversation")
    public ResponseEntity<ConversationResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationService.create(userDetails.getUsername(), request));
    }

    @GetMapping
    @Operation(summary = "List authenticated user's conversations")
    public ResponseEntity<List<ConversationResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(conversationService.listForUser(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a conversation by ID")
    public ResponseEntity<ConversationResponse> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(conversationService.getById(userDetails.getUsername(), id));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get messages for a conversation")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(conversationService.getMessages(userDetails.getUsername(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a conversation")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        conversationService.delete(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
