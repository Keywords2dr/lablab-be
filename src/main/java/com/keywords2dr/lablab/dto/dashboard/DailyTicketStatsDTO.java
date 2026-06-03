package com.keywords2dr.lablab.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Thống kê phiếu mượn theo từng ngày — dùng cho biểu đồ 7 ngày dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTicketStatsDTO {

    private LocalDate date;

    /** Nhãn hiển thị ngắn cho FE: "T2", "T3", …, "CN" */
    private String dayLabel;

    /** Số phiếu đã được duyệt hoàn toàn (APPROVED) trong ngày */
    private long approved;

    /** Số phiếu bị từ chối (REJECTED) trong ngày */
    private long rejected;

    /**
     * Số phiếu "đang xử lý" trong ngày:
     * PENDING_OWNER + PENDING_ADMIN + BORROWED + PENDING_RETURN
     */
    private long pending;
}