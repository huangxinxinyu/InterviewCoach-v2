package com.xinyu.InterviewCoach_v2.dto.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xinyu.InterviewCoach_v2.enums.MessageType;

import java.time.LocalDateTime; /**
 * 消息DTO
 */
public class MessageDTO {
    private Long id;
    private Long sessionId;
    private MessageType type;
    private String text;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public MessageDTO() {}

    public MessageDTO(Long id, Long sessionId, MessageType type, String text, LocalDateTime createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.type = type;
        this.text = text;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}