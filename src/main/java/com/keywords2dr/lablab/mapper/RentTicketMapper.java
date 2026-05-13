package com.keywords2dr.lablab.mapper;

import com.keywords2dr.lablab.dto.ticket.RentTicketDetailResponse;
import com.keywords2dr.lablab.dto.ticket.RentTicketResponse;
import com.keywords2dr.lablab.dto.ticket.RentTicketSummaryResponse;
import com.keywords2dr.lablab.entity.RentTicket;
import com.keywords2dr.lablab.entity.RentTicketDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RentTicketMapper {

    // ── FULL RESPONSE ─────────────────────────────────────────────────────────

    @Mapping(source = "requester.userId",                   target = "requesterId")
    @Mapping(source = "requester.username",                 target = "requesterName")
    @Mapping(source = "requester.role",                     target = "requesterRole")
    @Mapping(source = "fromRoom.roomId",                    target = "roomId")
    @Mapping(source = "fromRoom.roomName",                  target = "roomName")
    @Mapping(source = "ticketType",                         target = "ticketType")
    @Mapping(source = "status",                             target = "status")
    @Mapping(source = "ownerApprovedBy.userId",             target = "ownerApprovedById")
    @Mapping(source = "ownerApprovedBy.profile.fullName",   target = "ownerApprovedByName")
    @Mapping(source = "adminApprovedBy.userId",             target = "adminApprovedById")
    @Mapping(source = "adminApprovedBy.profile.fullName",   target = "adminApprovedByName")
    @Mapping(source = "ticketDetails",                      target = "items")
    RentTicketResponse toResponse(RentTicket ticket);

    // ── SUMMARY RESPONSE (danh sách, không kèm items) ────────────────────────

    @Mapping(source = "requester.username",     target = "requesterName")
    @Mapping(source = "requester.role",         target = "requesterRole")
    @Mapping(source = "fromRoom.roomName",      target = "roomName")
    @Mapping(source = "ticketType",             target = "ticketType")
    @Mapping(source = "status",                 target = "status")
    @Mapping(
            target = "itemCount",
            expression = "java(ticket.getTicketDetails() != null ? ticket.getTicketDetails().size() : 0)"
    )
    RentTicketSummaryResponse toSummaryResponse(RentTicket ticket);

    List<RentTicketSummaryResponse> toSummaryResponseList(List<RentTicket> tickets);

    // ── DETAIL LINE ───────────────────────────────────────────────────────────

    @Mapping(source = "item.itemId",    target = "itemId")
    @Mapping(source = "item.itemCode",  target = "itemCode")
    @Mapping(source = "item.name",      target = "itemName")
    @Mapping(source = "item.unit",      target = "itemUnit")
    @Mapping(source = "returnStatus",   target = "returnStatus")
    RentTicketDetailResponse toDetailResponse(RentTicketDetail detail);

    List<RentTicketDetailResponse> toDetailResponseList(List<RentTicketDetail> details);
}