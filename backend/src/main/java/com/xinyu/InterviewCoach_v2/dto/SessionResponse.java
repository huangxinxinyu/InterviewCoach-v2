package com.xinyu.InterviewCoach_v2.dto;

import com.xinyu.InterviewCoach_v2.enums.InterviewState; /**
 * 会话响应DTO
 */
public class SessionResponse {
    private boolean success;
    private String message;
    private SessionDTO session;
    private InterviewState currentState;
    private boolean chatInputEnabled;

    public SessionResponse() {}

    public SessionResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public SessionResponse(boolean success, SessionDTO session, InterviewState currentState, boolean chatInputEnabled) {
        this.success = success;
        this.session = session;
        this.currentState = currentState;
        this.chatInputEnabled = chatInputEnabled;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public SessionDTO getSession() { return session; }
    public void setSession(SessionDTO session) { this.session = session; }

    public InterviewState getCurrentState() { return currentState; }
    public void setCurrentState(InterviewState currentState) { this.currentState = currentState; }

    public boolean isChatInputEnabled() { return chatInputEnabled; }
    public void setChatInputEnabled(boolean chatInputEnabled) { this.chatInputEnabled = chatInputEnabled; }
}
