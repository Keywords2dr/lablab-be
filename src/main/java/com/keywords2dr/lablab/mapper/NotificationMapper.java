package com.keywords2dr.lablab.mapper;

import com.keywords2dr.lablab.dto.notification.NotificationResponseDTO;
import com.keywords2dr.lablab.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponseDTO toResponse(Notification entity);
}