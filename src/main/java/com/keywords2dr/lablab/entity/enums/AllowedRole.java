package com.keywords2dr.lablab.entity.enums;

import com.keywords2dr.lablab.exception.BadRequestException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum AllowedRole {

    ADMIN,
    TEACHER,
    STUDENT;

    private static final Set<String> VALID_NAMES =
            Arrays.stream(values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

    public static String validateAndNormalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Role không được để trống!");
        }

        String normalized = raw.trim().toUpperCase();

        if (!VALID_NAMES.contains(normalized)) {
            throw new BadRequestException(
                    String.format("Role '%s' không hợp lệ! Chỉ chấp nhận: %s",
                            raw, String.join(", ", VALID_NAMES))
            );
        }

        return normalized;
    }

    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) return false;
        return VALID_NAMES.contains(raw.trim().toUpperCase());
    }
}
