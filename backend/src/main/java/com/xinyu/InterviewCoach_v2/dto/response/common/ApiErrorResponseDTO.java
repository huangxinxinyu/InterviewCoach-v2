package com.xinyu.InterviewCoach_v2.dto.response.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 统一API错误响应DTO
 */
public class ApiErrorResponseDTO {
    private boolean success = false;
    private String error;
    private String code;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public ApiErrorResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiErrorResponseDTO(String error) {
        this();
        this.error = error;
        this.message = error;
    }

    public ApiErrorResponseDTO(String error, String code) {
        this();
        this.error = error;
        this.code = code;
        this.message = error;
    }

    public ApiErrorResponseDTO(String error, String code, String message) {
        this();
        this.error = error;
        this.code = code;
        this.message = message;
    }

    // Builder pattern
    public static ApiErrorResponseDTO builder() {
        return new ApiErrorResponseDTO();
    }

    public ApiErrorResponseDTO error(String error) {
        this.error = error;
        return this;
    }

    public ApiErrorResponseDTO code(String code) {
        this.code = code;
        return this;
    }

    public ApiErrorResponseDTO message(String message) {
        this.message = message;
        return this;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}