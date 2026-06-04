package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.chat.ChatRequest;
import com.keywords2dr.lablab.dto.chat.ChatResponse;
import com.keywords2dr.lablab.security.SecurityUtils;
import com.keywords2dr.lablab.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        var currentUser = Objects.requireNonNull(SecurityUtils.getCurrentUser());

        String role = currentUser.getAuthorities()
                .iterator().next()
                .getAuthority()
                .replace("ROLE_", "");

        UUID userId = currentUser.getId();

        return ResponseEntity.ok(chatService.chat(request, role, userId));
    }
}