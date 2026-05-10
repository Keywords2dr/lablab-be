package com.keywords2dr.lablab.dto.room;

import lombok.Data;
import java.util.UUID;

@Data
public class RoomStaffResponseDTO {
    private UUID id;
    private UUID roomId;
    private UUID userId;
    private String username;
    private String fullName;
    private String role;
}