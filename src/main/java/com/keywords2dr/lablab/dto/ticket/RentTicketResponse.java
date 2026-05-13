package com.keywords2dr.lablab.dto.ticket;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class RentTicketResponse {

    private UUID ticketId;

    // Người mượn
    private UUID requesterId;
    private String requesterName;
    private String requesterRole;       // TEACHER | STUDENT

    // Phòng
    private UUID roomId;
    private String roomName;

    // Loại & trạng thái
    private String ticketType;
    private String status;

    // Mục đích
    private String purposeType;
    private String subjectName;
    private String lessonDetail;
    private String classCode;

    // Thời gian
    private LocalDateTime borrowDate;
    private LocalDateTime expectedReturnDate;
    private LocalDateTime actualReturnDate;

    // Duyệt Teacher
    private UUID ownerApprovedById;
    private String ownerApprovedByName;
    private LocalDateTime ownerApprovedAt;

    // Duyệt Admin
    private UUID adminApprovedById;
    private String adminApprovedByName;
    private LocalDateTime adminApprovedAt;

    // Từ chối — FIX #2: tách riêng người từ chối
    private UUID rejectedById;
    private String rejectedByName;
    private String rejectedReason;
    private LocalDateTime rejectedAt;

    private LocalDateTime createdAt;

    // Chi tiết hóa chất (null nếu ROOM_ONLY)
    private List<RentTicketDetailResponse> items;
}