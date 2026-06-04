package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.chat.ChatRequest;
import com.keywords2dr.lablab.dto.chat.ChatResponse;

import java.util.UUID;

public interface ChatService {
    ChatResponse chat(ChatRequest request, String role, UUID userId);
}