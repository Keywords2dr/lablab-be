package com.keywords2dr.lablab.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityFeedItemDTO {

    private UUID logId;

    /** Icon category: TICKET | ROOM | CHEMICAL | USER | INVENTORY | SYSTEM */
    private String category;

    private String description;

    private String actorName;

    private String actorRole;

    private LocalDateTime createdAt;

    /** Thời gian tương đối, VD: "2 phút trước" */
    private String timeAgo;
}