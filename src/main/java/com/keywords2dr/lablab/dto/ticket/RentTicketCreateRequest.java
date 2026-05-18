package com.keywords2dr.lablab.dto.ticket;

import com.keywords2dr.lablab.entity.enums.PurposeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class RentTicketCreateRequest {

    @NotNull(message = "Phòng mượn không được để trống!")
    private UUID roomId;

    @NotBlank(message = "Loại phiếu không được để trống!")
    private String ticketType;      // ROOM_ONLY | CHEMICAL_ONLY

    // Thông tin mục đích
    private PurposeType purposeType;     // TEACHING | RESEARCH | PERSONAL | OTHER
    private String subjectName;
    private String lessonDetail;
    private String classCode;

    // Thời gian — theo giờ, dạng "2025-06-01T08:00:00"
    @NotNull(message = "Thời gian bắt đầu mượn không được để trống!")
    private LocalDateTime borrowDate;

    @NotNull(message = "Thời gian dự kiến trả không được để trống!")
    @Future(message = "Thời gian trả phải sau thời điểm hiện tại!")
    private LocalDateTime expectedReturnDate;

    // Danh sách hóa chất mượn — bắt buộc nếu ticketType != ROOM_ONLY
    @Valid
    private List<RentTicketDetailRequest> items;
}