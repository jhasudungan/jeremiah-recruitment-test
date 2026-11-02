package com.kopnus.jeremiah_recruitment_test.dto;

import java.util.List;

public record ApiError(
        String errorCode,
        String message,
        List<String> details
) {
    public static ApiError of(String errorCode, String message) {
        return new ApiError(errorCode, message, List.of());
    }

    public static ApiError of(String errorCode, String message, List<String> details) {
        return new ApiError(errorCode, message, details);
    }
}
