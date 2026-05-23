package com.restrosync.backend.dto;

/**
 * DTO for API responses
 */
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String timestamp;

    public ApiResponse(boolean success, String message, T data, String timestamp) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, String.valueOf(System.currentTimeMillis()));
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, String.valueOf(System.currentTimeMillis()));
    }
}
