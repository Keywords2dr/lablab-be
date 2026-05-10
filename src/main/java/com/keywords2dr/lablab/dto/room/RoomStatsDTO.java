package com.keywords2dr.lablab.dto.room;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomStatsDTO {
    private long totalRooms;
    private long roomsWithoutStaff;
    private long totalActiveTeachers;
}