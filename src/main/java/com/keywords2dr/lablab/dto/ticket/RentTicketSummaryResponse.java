package com.keywords2dr.lablab.dto.ticket;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RentTicketSummaryResponse {

    private UUID ticketId;

    // Người mượn
    private String requesterName;
    private String requesterRole;

    // Phòng
    private String roomName;

    // Loại & trạng thái
    private String ticketType;
    private String status;

    // Mục đích
    private String purposeType;
    private String subjectName;

    // Thời gian
    private LocalDateTime borrowDate;
    private LocalDateTime expectedReturnDate;

    private LocalDateTime createdAt;

    // Số lượng hóa chất trong phiếu (0 nếu ROOM_ONLY)
    private int itemCount;
}