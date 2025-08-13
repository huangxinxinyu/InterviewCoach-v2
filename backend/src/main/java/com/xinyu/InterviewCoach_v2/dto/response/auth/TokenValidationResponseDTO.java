package com.xinyu.InterviewCoach_v2.dto.response.auth;

import com.xinyu.InterviewCoach_v2.dto.UserDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Token验证响应DTO
 */
public class TokenValidationResponseDTO {
    private boolean valid;
    private String message;
    private UserDTO user;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public TokenValidationResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public TokenValidationResponseDTO(boolean valid, String message) {
        this();
        this.valid = valid;
        this.message = message;
    }

    public TokenValidationResponseDTO(boolean valid, String message, UserDTO user) {
        this();
        this.valid = valid;
        this.message = message;
        this.user = user;
    }

    // Builder pattern
    public static TokenValidationResponseDTO builder() {
        return new TokenValidationResponseDTO();
    }

    public TokenValidationResponseDTO valid(boolean valid) {
        this.valid = valid;
        return this;
    }

    public TokenValidationResponseDTO message(String message) {
        this.message = message;
        return this;
    }

    public TokenValidationResponseDTO user(UserDTO user) {
        this.user = user;
        return this;
    }

    // Getters and Setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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