package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.notification.NotificationResponseDTO;
import com.keywords2dr.lablab.entity.Notification;
import com.keywords2dr.lablab.mapper.NotificationMapper;
import com.keywords2dr.lablab.repository.NotificationRepository;
import com.keywords2dr.lablab.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getMyNotifications(UUID userId, Pageable pageable) {
        return notificationRepository
                .findByUser_UserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo!"));

        // Chặn user đánh dấu thông báo của người khác
        if (!notification.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thao tác thông báo này!");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}