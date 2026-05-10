package com.keywords2dr.lablab.dto.chemical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteChemicalResponse {

    private String message;

    private boolean hasActiveInventory;

    private List<AffectedRoom> affectedRooms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AffectedRoom {
        private String roomName;
        private BigDecimal remainingQuantity;
        private String unit;
    }
}