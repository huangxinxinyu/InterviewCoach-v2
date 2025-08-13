package com.xinyu.InterviewCoach_v2.dto.response.tag;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 清理操作响应DTO
 */
public class CleanupResponseDTO {
    private boolean success = true;
    private String message;
    private int deletedCount;
    private String operation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public CleanupResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public CleanupResponseDTO(String message, int deletedCount) {
        this();
        this.message = message;
        this.deletedCount = deletedCount;
    }

    public CleanupResponseDTO(String message, int deletedCount, String operation) {
        this();
        this.message = message;
        this.deletedCount = deletedCount;
        this.operation = operation;
    }

    // Builder pattern
    public static CleanupResponseDTO builder() {
        return new CleanupResponseDTO();
    }

    public CleanupResponseDTO message(String message) {
        this.message = message;
        return this;
    }

    public CleanupResponseDTO deletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
        return this;
    }

    public CleanupResponseDTO operation(String operation) {
        this.operation = operation;
        return this;
    }

    public CleanupResponseDTO success(boolean success) {
        this.success = success;
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

    public int getDeletedCount() {
        return deletedCount;
    }

    public void setDeletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}