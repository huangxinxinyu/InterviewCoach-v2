package com.xinyu.InterviewCoach_v2.dto.response.auth;

import com.xinyu.InterviewCoach_v2.dto.UserDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 注册响应DTO
 */
public class RegisterResponseDTO {
    private boolean success;
    private String message;
    private String token;
    private UserDTO user;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public RegisterResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public RegisterResponseDTO(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public RegisterResponseDTO(boolean success, String message, String token, UserDTO user) {
        this();
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
    }

    // Builder pattern
    public static RegisterResponseDTO builder() {
        return new RegisterResponseDTO();
    }

    public RegisterResponseDTO success(boolean success) {
        this.success = success;
        return this;
    }

    public RegisterResponseDTO message(String message) {
        this.message = message;
        return this;
    }

    public RegisterResponseDTO token(String token) {
        this.token = token;
        return this;
    }

    public RegisterResponseDTO user(UserDTO user) {
        this.user = user;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}