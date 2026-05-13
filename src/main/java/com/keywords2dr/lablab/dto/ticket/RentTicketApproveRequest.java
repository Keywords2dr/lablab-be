package com.keywords2dr.lablab.dto.ticket;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RentTicketApproveRequest {

    @NotNull(message = "Quyết định không được để trống!")
    private Boolean approved;           // true = duyệt, false = từ chối

    private String rejectedReason;      // Bắt buộc nếu approved = false, validate ở service
}