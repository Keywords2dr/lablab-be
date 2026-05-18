package com.keywords2dr.lablab.dto.report;

import com.keywords2dr.lablab.entity.enums.ReportType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReportTicketResponse {
    private UUID reportId;
    private ReportType reportType;

    private UUID reporterId;
    private String reporterName;

    private UUID roomId;
    private String roomName;       // null nếu CHEMICAL

    private UUID itemId;
    private String itemName;       // null nếu ROOM

    private String description;
    private LocalDateTime createdAt;
}