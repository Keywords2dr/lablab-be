package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.dashboard.ActivityFeedItemDTO;
import com.keywords2dr.lablab.dto.dashboard.DailyTicketStatsDTO;
import com.keywords2dr.lablab.dto.dashboard.RoomCurrentUsageDTO;

import java.util.List;

public interface DashboardService {

    /*Thống kê phiếu mượn cho 7 ngày gần nhất.*/
    List<DailyTicketStatsDTO> getWeeklyTicketStats();

    /*Trạng thái sử dụng thực tế của tất cả phòng Lab hiện tại.*/
    List<RoomCurrentUsageDTO> getCurrentRoomUsage();

    /* Activity feed thân thiện cho Admin Dashboard. */
    List<ActivityFeedItemDTO> getActivityFeed(int limit);
}