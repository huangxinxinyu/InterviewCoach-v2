package com.xinyu.InterviewCoach_v2.dto.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xinyu.InterviewCoach_v2.enums.InterviewState;
import com.xinyu.InterviewCoach_v2.enums.SessionMode;

import java.time.LocalDateTime;

/**
 * 会话DTO - 移动到core包
 */
public class SessionDTO {
    private Long id;
    private Long userId;
    private SessionMode mode;
    private Integer expectedQuestionCount;
    private Integer askedQuestionCount;
    private Integer completedQuestionCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endedAt;

    private Boolean isActive;
    private InterviewState currentState;

    public SessionDTO() {}

    public SessionDTO(Long id, Long userId, SessionMode mode, Integer expectedQuestionCount,
                      Integer askedQuestionCount, Integer completedQuestionCount,
                      LocalDateTime startedAt, LocalDateTime endedAt, Boolean isActive) {
        this.id = id;
        this.userId = userId;
        this.mode = mode;
        this.expectedQuestionCount = expectedQuestionCount;
        this.askedQuestionCount = askedQuestionCount;
        this.completedQuestionCount = completedQuestionCount;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public SessionMode getMode() { return mode; }
    public void setMode(SessionMode mode) { this.mode = mode; }

    public Integer getExpectedQuestionCount() { return expectedQuestionCount; }
    public void setExpectedQuestionCount(Integer expectedQuestionCount) { this.expectedQuestionCount = expectedQuestionCount; }

    public Integer getAskedQuestionCount() { return askedQuestionCount; }
    public void setAskedQuestionCount(Integer askedQuestionCount) { this.askedQuestionCount = askedQuestionCount; }

    public Integer getCompletedQuestionCount() { return completedQuestionCount; }
    public void setCompletedQuestionCount(Integer completedQuestionCount) { this.completedQuestionCount = completedQuestionCount; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public InterviewState getCurrentState() { return currentState; }
    public void setCurrentState(InterviewState currentState) { this.currentState = currentState; }
}