package com.keywords2dr.lablab.dto.ticket;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@Data
@Builder
public class ProblematicReturnPayload {

    private UUID ticketId;
    private String roomName;
    private String requesterName;
    private List<IssueItem> issues;

    @Data
    @Builder
    public static class IssueItem {
        private String itemName;
        private BigDecimal quantityBorrowed;
        private BigDecimal quantityReturned;
        private String returnStatus;   // DAMAGED | LOST | PARTIAL
        private String returnNote;
    }
}