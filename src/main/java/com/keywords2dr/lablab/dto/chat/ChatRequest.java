package com.keywords2dr.lablab.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {

    @NotBlank(message = "Tin nhắn không được để trống!")
    @Size(max = 1000, message = "Tin nhắn không được vượt quá 1000 ký tự!")
    private String message;

    private List<ChatTurn> history;

    @Data
    public static class ChatTurn {
        private String role;
        private String content;
    }
}