package com.xinyu.InterviewCoach_v2.dto.response.chat;

import com.xinyu.InterviewCoach_v2.dto.SessionDTO;
import com.xinyu.InterviewCoach_v2.enums.InterviewState;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 会话操作响应DTO
 */
public class SessionResponseDTO {
    private boolean success;
    private String message;
    private SessionDTO session;
    private InterviewState currentState;
    private boolean chatInputEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public SessionResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public SessionResponseDTO(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public SessionResponseDTO(boolean success, SessionDTO session, InterviewState currentState, boolean chatInputEnabled) {
        this();
        this.success = success;
        this.session = session;
        this.currentState = currentState;
        this.chatInputEnabled = chatInputEnabled;
    }

    // Builder pattern
    public static SessionResponseDTO builder() {
        return new SessionResponseDTO();
    }

    public SessionResponseDTO success(boolean success) {
        this.success = success;
        return this;
    }

    public SessionResponseDTO message(String message) {
        this.message = message;
        return this;
    }

    public SessionResponseDTO session(SessionDTO session) {
        this.session = session;
        return this;
    }

    public SessionResponseDTO currentState(InterviewState currentState) {
        this.currentState = currentState;
        return this;
    }

    public SessionResponseDTO chatInputEnabled(boolean chatInputEnabled) {
        this.chatInputEnabled = chatInputEnabled;
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

    public SessionDTO getSession() {
        return session;
    }

    public void setSession(SessionDTO session) {
        this.session = session;
    }

    public InterviewState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(InterviewState currentState) {
        this.currentState = currentState;
    }

    public boolean isChatInputEnabled() {
        return chatInputEnabled;
    }

    public void setChatInputEnabled(boolean chatInputEnabled) {
        this.chatInputEnabled = chatInputEnabled;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}