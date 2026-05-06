package com.keywords2dr.lablab.event.listener;

import com.keywords2dr.lablab.entity.Notification;
import com.keywords2dr.lablab.entity.User;
import com.keywords2dr.lablab.event.NotificationEvent;
import com.keywords2dr.lablab.repository.NotificationRepository;
import com.keywords2dr.lablab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            User userRef = userRepository.getReferenceById(event.getUserId());

            Notification notification = Notification.builder()
                    .user(userRef)
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .type(event.getType())
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
            log.info("Đã tạo thông báo cho User ID: {}", event.getUserId());

        } catch (Exception e) {
            log.error("Lỗi khi tạo thông báo cho User ID [{}]: {}", event.getUserId(), e.getMessage());
        }
    }
}