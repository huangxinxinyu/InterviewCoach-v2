package com.xinyu.InterviewCoach_v2.dto;

import com.xinyu.InterviewCoach_v2.enums.InterviewState; /**
 * 聊天响应DTO
 */
public class ChatResponse {
    private boolean success;
    private String message;
    private MessageDTO aiMessage;
    private InterviewState currentState;
    private boolean chatInputEnabled;
    private SessionDTO session;

    public ChatResponse() {}

    public ChatResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ChatResponse(boolean success, MessageDTO aiMessage, InterviewState currentState, boolean chatInputEnabled) {
        this.success = success;
        this.aiMessage = aiMessage;
        this.currentState = currentState;
        this.chatInputEnabled = chatInputEnabled;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public MessageDTO getAiMessage() { return aiMessage; }
    public void setAiMessage(MessageDTO aiMessage) { this.aiMessage = aiMessage; }

    public InterviewState getCurrentState() { return currentState; }
    public void setCurrentState(InterviewState currentState) { this.currentState = currentState; }

    public boolean isChatInputEnabled() { return chatInputEnabled; }
    public void setChatInputEnabled(boolean chatInputEnabled) { this.chatInputEnabled = chatInputEnabled; }

    public SessionDTO getSession() { return session; }
    public void setSession(SessionDTO session) { this.session = session; }
}
