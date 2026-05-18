package com.keywords2dr.lablab.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReportType {
    ROOM,
    CHEMICAL;

    @JsonCreator
    public static ReportType fromString(String value) {
        if (value == null) return null;
        return ReportType.valueOf(value.trim().toUpperCase());
    }
}