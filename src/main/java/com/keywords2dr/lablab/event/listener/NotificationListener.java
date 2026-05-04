package com.keywords2dr.lablab.event.listener;

import com.keywords2dr.lablab.entity.Notification;
import com.keywords2dr.lablab.entity.User;
import com.keywords2dr.lablab.event.NotificationEvent;
import com.keywords2dr.lablab.repository.NotificationRepository;
import com.keywords2dr.lablab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            User user = userRepository.findById(event.getUserId()).orElse(null);
            if (user == null) return;

            Notification notification = Notification.builder()
                    .user(user)
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .type(event.getType())
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
            log.info("Đã tạo thông báo cho User: {}", user.getUsername());

        } catch (Exception e) {
            log.error("Lỗi khi tạo thông báo: {}", e.getMessage());
        }
    }
}