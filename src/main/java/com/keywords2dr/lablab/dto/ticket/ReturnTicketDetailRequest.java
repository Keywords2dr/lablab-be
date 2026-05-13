package com.keywords2dr.lablab.dto.ticket;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ReturnTicketDetailRequest {

    @NotNull(message = "ID chi tiết phiếu không được để trống!")
    private UUID detailId;

    @NotNull(message = "Số lượng thực trả không được để trống!")
    @DecimalMin(value = "0.00", message = "Số lượng trả không được âm!")
    private BigDecimal quantityReturned;

    @NotBlank(message = "Trạng thái trả không được để trống!")
    private String returnStatus;    // RETURNED | PARTIAL | DAMAGED | LOST

    private String returnNote;      // Bắt buộc nếu returnStatus = DAMAGED | LOST | PARTIAL
}