package com.keywords2dr.lablab.entity.enums;

public enum ReturnStatus {
    NOT_RETURNED,   // Chưa trả
    RETURNED,       // Đã trả đủ
    PARTIAL,        // Trả thiếu số lượng
    DAMAGED,        // Trả nhưng bị hỏng
    LOST            // Mất, không trả được
}