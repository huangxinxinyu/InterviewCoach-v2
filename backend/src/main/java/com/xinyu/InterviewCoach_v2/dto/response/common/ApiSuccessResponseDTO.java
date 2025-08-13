package com.xinyu.InterviewCoach_v2.dto.response.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 统一API成功响应DTO
 */
public class ApiSuccessResponseDTO<T> {
    private boolean success = true;
    private String message;
    private T data;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public ApiSuccessResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiSuccessResponseDTO(String message) {
        this();
        this.message = message;
    }

    public ApiSuccessResponseDTO(T data) {
        this();
        this.data = data;
    }

    public ApiSuccessResponseDTO(String message, T data) {
        this();
        this.message = message;
        this.data = data;
    }

    // Builder pattern
    public static <T> ApiSuccessResponseDTO<T> builder() {
        return new ApiSuccessResponseDTO<>();
    }

    public ApiSuccessResponseDTO<T> message(String message) {
        this.message = message;
        return this;
    }

    public ApiSuccessResponseDTO<T> data(T data) {
        this.data = data;
        return this;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}