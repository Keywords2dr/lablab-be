package com.keywords2dr.lablab.dto.report;

import com.keywords2dr.lablab.dto.report.validation.ValidReportTicket;
import com.keywords2dr.lablab.entity.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@ValidReportTicket
@Data
public class ReportTicketRequest {

    @NotNull(message = "Loại báo cáo không được để trống")
    private ReportType reportType;

    @NotNull(message = "Vui lòng chọn phòng cần báo cáo")
    private UUID roomId;

    private UUID itemId;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;
}