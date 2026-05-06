package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.notification.NotificationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    Page<NotificationResponseDTO> getMyNotifications(UUID userId, Pageable pageable);

    long countUnread(UUID userId);

    // Đánh dấu 1 thông báo đã đọc
    void markAsRead(UUID notificationId, UUID userId);

    // Đánh dấu tất cả thông báo của user là đã đọc
    void markAllAsRead(UUID userId);
}