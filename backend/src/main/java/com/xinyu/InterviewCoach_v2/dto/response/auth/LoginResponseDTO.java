package com.xinyu.InterviewCoach_v2.dto.response.auth;

import com.xinyu.InterviewCoach_v2.dto.UserDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 登录响应DTO
 */
public class LoginResponseDTO {
    private boolean success;
    private String message;
    private String token;
    private UserDTO user;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public LoginResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public LoginResponseDTO(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public LoginResponseDTO(boolean success, String message, String token, UserDTO user) {
        this();
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
    }

    // Builder pattern
    public static LoginResponseDTO builder() {
        return new LoginResponseDTO();
    }

    public LoginResponseDTO success(boolean success) {
        this.success = success;
        return this;
    }

    public LoginResponseDTO message(String message) {
        this.message = message;
        return this;
    }

    public LoginResponseDTO token(String token) {
        this.token = token;
        return this;
    }

    public LoginResponseDTO user(UserDTO user) {
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