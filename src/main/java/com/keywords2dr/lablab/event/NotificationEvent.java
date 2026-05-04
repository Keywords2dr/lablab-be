package com.keywords2dr.lablab.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class NotificationEvent {
    private UUID userId;
    private String title;      // Tiêu đề
    private String message;    // Nội dung
    private String type;       // Phân loại
}