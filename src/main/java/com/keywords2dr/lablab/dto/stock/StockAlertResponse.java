package com.keywords2dr.lablab.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertResponse {

    private UUID itemId;
    private String itemCode;
    private String itemName;
    private String unit;

    /**
     * Tổng tồn kho toàn hệ thống (tất cả phòng).
     */
    private BigDecimal totalQuantity;

    /**
     * Ngưỡng cảnh báo đã cài đặt.
     */
    private BigDecimal minQuantity;

    /**
     * OUT_OF_STOCK  : totalQuantity == 0
     * LOW_STOCK     : 0 < totalQuantity < minQuantity
     */
    private AlertLevel alertLevel;

    /**
     * Chi tiết tồn kho từng phòng.
     */
    private List<RoomStock> roomStocks;

    public enum AlertLevel {
        OUT_OF_STOCK,
        LOW_STOCK
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomStock {
        private UUID roomId;
        private String roomName;
        private BigDecimal quantity;
        private BigDecimal lockedQuantity;
        private BigDecimal availableQuantity;
    }
}