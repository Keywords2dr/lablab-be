package com.keywords2dr.lablab.dto.chemical;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class GlobalInventoryResponse {
    private UUID itemId;
    private String itemCode;
    private String name;
    private String unit;

    private BigDecimal grandTotal; // Tổng số lượng trên tất cả các phòng
    private List<RoomStockDetail> roomDetails; // Chi tiết phân bổ

    // Class nội bộ để chứa thông tin chi tiết từng phòng
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomStockDetail {
        private String roomName;
        private BigDecimal quantity;
    }
}