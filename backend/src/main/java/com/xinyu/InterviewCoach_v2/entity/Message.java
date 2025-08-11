package com.xinyu.InterviewCoach_v2.entity;

import com.xinyu.InterviewCoach_v2.enums.MessageType;
import java.time.LocalDateTime;

/**
 * 会话消息实体类
 */
public class Message {

    private Long id;
    private Long sessionId;
    private MessageType type;
    private String text;
    private LocalDateTime createdAt;

    public Message() {}

    public Message(Long sessionId, MessageType type, String text) {
        this.sessionId = sessionId;
        this.type = type;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", sessionId=" + sessionId +
                ", type=" + type +
                ", text='" + text + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

