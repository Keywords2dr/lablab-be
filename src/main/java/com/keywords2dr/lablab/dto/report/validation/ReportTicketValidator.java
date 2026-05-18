package com.keywords2dr.lablab.dto.report.validation;

import com.keywords2dr.lablab.dto.report.ReportTicketRequest;
import com.keywords2dr.lablab.entity.enums.ReportType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReportTicketValidator
        implements ConstraintValidator<ValidReportTicket, ReportTicketRequest> {

    @Override
    public boolean isValid(ReportTicketRequest request, ConstraintValidatorContext ctx) {
        if (request.getReportType() == null) return true;

        ctx.disableDefaultConstraintViolation();

        return switch (request.getReportType()) {
            case CHEMICAL -> validateChemical(request, ctx);
            case ROOM     -> validateRoom(request, ctx);
        };
    }

    private boolean validateChemical(ReportTicketRequest request, ConstraintValidatorContext ctx) {
        if (request.getItemId() == null) {
            ctx.buildConstraintViolationWithTemplate("Vui lòng chọn hóa chất cần báo cáo")
                    .addPropertyNode("itemId")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean validateRoom(ReportTicketRequest request, ConstraintValidatorContext ctx) {
        if (request.getItemId() != null) {
            ctx.buildConstraintViolationWithTemplate("Báo cáo phòng không cần chọn hóa chất")
                    .addPropertyNode("itemId")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}