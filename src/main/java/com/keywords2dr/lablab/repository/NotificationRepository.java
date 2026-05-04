package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    // Lấy thông báo của 1 user, sắp xếp mới nhất lên đầu
    List<Notification> findAllByUser_UserIdOrderByCreatedAtDesc(UUID userId);

    // Đếm số thông báo chưa đọc
    long countByUser_UserIdAndIsReadFalse(UUID userId);
}