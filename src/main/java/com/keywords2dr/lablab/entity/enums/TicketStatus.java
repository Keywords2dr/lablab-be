package com.keywords2dr.lablab.entity.enums;

public enum TicketStatus {
    PENDING_OWNER,    // Chờ Teacher (chủ phòng) duyệt
    PENDING_ADMIN,    // Teacher đã duyệt, chờ Admin duyệt (chỉ khi có hóa chất)
    APPROVED,         // Đã được duyệt hoàn toàn
    REJECTED,         // Bị từ chối (Teacher hoặc Admin)
    BORROWED,         // Đang mượn
    PENDING_RETURN,   // Người mượn đã yêu cầu trả, chờ Teacher xác nhận
    RETURNED,         // Teacher đã xác nhận nhận lại đồ
    CANCELLED         // Người mượn tự hủy trước khi được duyệt
}