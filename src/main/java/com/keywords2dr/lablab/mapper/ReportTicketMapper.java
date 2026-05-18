package com.keywords2dr.lablab.mapper;

import com.keywords2dr.lablab.dto.report.ReportTicketResponse;
import com.keywords2dr.lablab.entity.ReportTicket;
import com.keywords2dr.lablab.util.UserNameResolver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ReportTicketMapper {

    @Mapping(source = "reporter.userId", target = "reporterId")
    @Mapping(source = "reporter", target = "reporterName", qualifiedByName = "resolveUserName")
    @Mapping(source = "room.roomId", target = "roomId")
    @Mapping(source = "room.roomName", target = "roomName")
    @Mapping(source = "item.itemId", target = "itemId")
    @Mapping(source = "item.name", target = "itemName")
    ReportTicketResponse toResponse(ReportTicket ticket);

    @Named("resolveUserName")
    default String resolveUserName(com.keywords2dr.lablab.entity.User reporter) {

        if (reporter == null) return null;
        return UserNameResolver.resolve(reporter);
    }
}