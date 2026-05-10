package com.keywords2dr.lablab.dto.room;

import lombok.Data;
import java.util.UUID;

@Data
public class RoomResponseDTO {
    private UUID roomId;
    private String roomName;
    private String description;
    private Boolean isActive;
    private int staffCount;
}