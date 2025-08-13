package com.xinyu.InterviewCoach_v2.dto.response.chat;

import com.xinyu.InterviewCoach_v2.dto.core.MessageDTO;
import com.xinyu.InterviewCoach_v2.dto.core.SessionDTO;
import com.xinyu.InterviewCoach_v2.enums.InterviewState;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 聊天消息响应DTO - 重构整理版本
 */
public class ChatMessageResponseDTO {
    private boolean success;
    private String message;
    private MessageDTO aiMessage;
    private InterviewState currentState;
    private boolean chatInputEnabled;
    private SessionDTO session;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public ChatMessageResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessageResponseDTO(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public ChatMessageResponseDTO(boolean success, MessageDTO aiMessage, InterviewState currentState, boolean chatInputEnabled) {
        this();
        this.success = success;
        this.aiMessage = aiMessage;
        this.currentState = currentState;
        this.chatInputEnabled = chatInputEnabled;
    }

    // Builder pattern
    public static ChatMessageResponseDTO builder() {
        return new ChatMessageResponseDTO();
    }

    public ChatMessageResponseDTO success(boolean success) {
        this.success = success;
        return this;
    }

    public ChatMessageResponseDTO message(String message) {
        this.message = message;
        return this;
    }

    public ChatMessageResponseDTO aiMessage(MessageDTO aiMessage) {
        this.aiMessage = aiMessage;
        return this;
    }

    public ChatMessageResponseDTO currentState(InterviewState currentState) {
        this.currentState = currentState;
        return this;
    }

    public ChatMessageResponseDTO chatInputEnabled(boolean chatInputEnabled) {
        this.chatInputEnabled = chatInputEnabled;
        return this;
    }

    public ChatMessageResponseDTO session(SessionDTO session) {
        this.session = session;
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

    public MessageDTO getAiMessage() {
        return aiMessage;
    }

    public void setAiMessage(MessageDTO aiMessage) {
        this.aiMessage = aiMessage;
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

    public SessionDTO getSession() {
        return session;
    }

    public void setSession(SessionDTO session) {
        this.session = session;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}