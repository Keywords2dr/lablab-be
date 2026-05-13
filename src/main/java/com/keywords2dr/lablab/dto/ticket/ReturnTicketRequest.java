package com.keywords2dr.lablab.dto.ticket;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReturnTicketRequest {

    // Với ROOM_ONLY thì items có thể null/rỗng
    @Valid
    private List<ReturnTicketDetailRequest> items;
}