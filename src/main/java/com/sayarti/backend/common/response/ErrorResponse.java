package com.sayarti.backend.common.response;

public record ErrorResponse(boolean success, ApiError error) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(false, new ApiError(code, message));
    }

    public static ErrorResponse of(
            String code, String message, java.util.Map<String, String> details) {
        return new ErrorResponse(false, new ApiError(code, message, details));
    }
}
