package com.keywords2dr.lablab.mapper;

import com.keywords2dr.lablab.dto.ticket.RentTicketDetailResponse;
import com.keywords2dr.lablab.dto.ticket.RentTicketResponse;
import com.keywords2dr.lablab.dto.ticket.RentTicketSummaryResponse;
import com.keywords2dr.lablab.entity.RentTicket;
import com.keywords2dr.lablab.entity.RentTicketDetail;
import com.keywords2dr.lablab.entity.enums.TicketType;
import com.keywords2dr.lablab.util.UserNameResolver;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RentTicketMapper {

    // ── Entity → Response đầy đủ ─────────────────────────────────────────────

    @Mapping(target = "requesterId",         source = "requester.userId")
    @Mapping(target = "requesterRole",       source = "requester.role")
    @Mapping(target = "roomId",              source = "fromRoom.roomId")
    @Mapping(target = "roomName",            source = "fromRoom.roomName")
    @Mapping(target = "ticketType",          expression = "java(ticket.getTicketType() != null ? ticket.getTicketType().name() : null)")
    @Mapping(target = "status",              expression = "java(ticket.getStatus() != null ? ticket.getStatus().name() : null)")
    @Mapping(target = "purposeType",         expression = "java(ticket.getPurposeType() != null ? ticket.getPurposeType().name() : null)")
    @Mapping(target = "ownerApprovedById",   source = "ownerApprovedBy.userId")
    @Mapping(target = "adminApprovedById",   source = "adminApprovedBy.userId")
    @Mapping(target = "rejectedById",        source = "rejectedBy.userId")
    @Mapping(target = "requesterName",       ignore = true)
    @Mapping(target = "ownerApprovedByName", ignore = true)
    @Mapping(target = "adminApprovedByName", ignore = true)
    @Mapping(target = "rejectedByName",      ignore = true)
    @Mapping(target = "items",               ignore = true)
    RentTicketResponse toResponse(RentTicket ticket);

    @AfterMapping
    default void fillResponseExtras(RentTicket ticket, @MappingTarget RentTicketResponse res) {
        if (ticket.getRequester() != null)
            res.setRequesterName(UserNameResolver.resolve(ticket.getRequester()));
        if (ticket.getOwnerApprovedBy() != null)
            res.setOwnerApprovedByName(UserNameResolver.resolve(ticket.getOwnerApprovedBy()));
        if (ticket.getAdminApprovedBy() != null)
            res.setAdminApprovedByName(UserNameResolver.resolve(ticket.getAdminApprovedBy()));
        if (ticket.getRejectedBy() != null)
            res.setRejectedByName(UserNameResolver.resolve(ticket.getRejectedBy()));

        if (ticket.getTicketType() != TicketType.ROOM_ONLY && ticket.getTicketDetails() != null) {
            res.setItems(ticket.getTicketDetails().stream()
                    .map(this::toDetailResponse)
                    .toList());
        } else {
            res.setItems(Collections.emptyList());
        }
    }

    // ── Entity → Summary ─────────────────────────────────────────────────────

    @Mapping(target = "requesterRole", source = "requester.role")
    @Mapping(target = "roomName",      source = "fromRoom.roomName")
    @Mapping(target = "ticketType",    expression = "java(ticket.getTicketType() != null ? ticket.getTicketType().name() : null)")
    @Mapping(target = "status",        expression = "java(ticket.getStatus() != null ? ticket.getStatus().name() : null)")
    @Mapping(target = "purposeType",   expression = "java(ticket.getPurposeType() != null ? ticket.getPurposeType().name() : null)")
    @Mapping(target = "note",          source = "note")
    @Mapping(target = "requesterName", ignore = true)
    @Mapping(target = "itemCount",     ignore = true)
    RentTicketSummaryResponse toSummaryResponse(RentTicket ticket);

    @AfterMapping
    default void fillSummaryExtras(RentTicket ticket, @MappingTarget RentTicketSummaryResponse res) {
        if (ticket.getRequester() != null)
            res.setRequesterName(UserNameResolver.resolve(ticket.getRequester()));
        res.setItemCount(ticket.getTicketDetails() != null
                ? ticket.getTicketDetails().size() : 0);
    }

    // ── RentTicketDetail → DetailResponse ────────────────────────────────────

    @Mapping(target = "itemId",       source = "item.itemId")
    @Mapping(target = "itemCode",     source = "item.itemCode")
    @Mapping(target = "itemName",     source = "item.name")
    @Mapping(target = "itemUnit",     source = "item.unit")
    @Mapping(target = "returnStatus", expression = "java(detail.getReturnStatus() != null ? detail.getReturnStatus().name() : null)")
    RentTicketDetailResponse toDetailResponse(RentTicketDetail detail);

    // ── List helpers ──────────────────────────────────────────────────────────

    default List<RentTicketSummaryResponse> toSummaryList(List<RentTicket> tickets) {
        if (tickets == null) return Collections.emptyList();
        return tickets.stream().map(this::toSummaryResponse).toList();
    }

    default List<RentTicketDetailResponse> toDetailList(List<RentTicketDetail> details) {
        if (details == null) return Collections.emptyList();
        return details.stream().map(this::toDetailResponse).toList();
    }
}