package com.keywords2dr.lablab.dto.ticket;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RentTicketDetailRequest {

    @NotNull(message = "ID hóa chất không được để trống!")
    private UUID itemId;

    @NotNull(message = "Số lượng mượn không được để trống!")
    @DecimalMin(value = "0.01", message = "Số lượng mượn phải lớn hơn 0!")
    private BigDecimal quantityBorrowed;
}