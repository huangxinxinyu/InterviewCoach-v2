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
    private String questionQueue;
    private Long currentQuestionId;
    private Integer queuePosition;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endedAt;

    private Boolean isActive;
    private InterviewState currentState;

    public SessionDTO() {
        this.queuePosition = 0;
    }

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

    public String getQuestionQueue() {
        return questionQueue;
    }

    public void setQuestionQueue(String questionQueue) {
        this.questionQueue = questionQueue;
    }

    public Long getCurrentQuestionId() {
        return currentQuestionId;
    }

    public void setCurrentQuestionId(Long currentQuestionId) {
        this.currentQuestionId = currentQuestionId;
    }

    public Integer getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(Integer queuePosition) {
        this.queuePosition = queuePosition;
    }

    /**
     * 检查队列是否完成
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isQueueCompleted() {
        return queuePosition != null && expectedQuestionCount != null &&
                queuePosition >= expectedQuestionCount;
    }

    /**
     * 获取队列进度百分比
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public double getQueueProgress() {
        if (expectedQuestionCount == null || expectedQuestionCount == 0) {
            return 0.0;
        }
        return (double) (queuePosition == null ? 0 : queuePosition) / expectedQuestionCount * 100;
    }
}