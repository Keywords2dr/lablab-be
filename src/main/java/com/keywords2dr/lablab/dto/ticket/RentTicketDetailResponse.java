package com.keywords2dr.lablab.dto.ticket;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RentTicketDetailResponse {
    private UUID detailId;
    private UUID itemId;
    private String itemCode;
    private String itemName;
    private String itemUnit;
    private BigDecimal quantityBorrowed;
    private BigDecimal quantityReturned;
    private String returnStatus;
    private String returnNote;
}