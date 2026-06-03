package com.keywords2dr.lablab.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Trạng thái sử dụng thực tế của 1 phòng Lab".
 * Trạng thái (status):
 *   "occupied"    — đang có phiếu BORROWED
 *   "available"   — phòng active, không có phiếu BORROWED
 *   "maintenance" — phòng isActive = false
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomCurrentUsageDTO {

    private UUID roomId;
    private String roomName;

    /**
     * occupied | available | maintenance
     */
    private String status;

    // ── Chỉ có giá trị khi status = "occupied" ──────────────

    private UUID activeTicketId;

    private String borrowerName;

    private LocalDateTime expectedReturnDate;

    private String ticketType;
}