package com.xinyu.InterviewCoach_v2.entity;

import com.xinyu.InterviewCoach_v2.enums.SessionMode;
import java.time.LocalDateTime;

/**
 * 面试会话实体类
 */
public class Session {

    private Long id;
    private Long userId;
    private SessionMode mode;
    private Integer expectedQuestionCount;
    private Integer askedQuestionCount;
    private Integer completedQuestionCount;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Boolean isActive;
    private String questionQueue;
    private Long currentQuestionId;
    private Integer queuePosition;

    public Session() {
        this.isActive = true;
        this.askedQuestionCount = 0;
        this.completedQuestionCount = 0;
        this.queuePosition = 0;
    }

    public Session(Long userId, SessionMode mode, Integer expectedQuestionCount) {
        this();
        this.userId = userId;
        this.mode = mode;
        this.expectedQuestionCount = expectedQuestionCount;
        this.startedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SessionMode getMode() {
        return mode;
    }

    public void setMode(SessionMode mode) {
        this.mode = mode;
    }

    public Integer getExpectedQuestionCount() {
        return expectedQuestionCount;
    }

    public void setExpectedQuestionCount(Integer expectedQuestionCount) {
        this.expectedQuestionCount = expectedQuestionCount;
    }

    public Integer getAskedQuestionCount() {
        return askedQuestionCount;
    }

    public void setAskedQuestionCount(Integer askedQuestionCount) {
        this.askedQuestionCount = askedQuestionCount;
    }

    public Integer getCompletedQuestionCount() {
        return completedQuestionCount;
    }

    public void setCompletedQuestionCount(Integer completedQuestionCount) {
        this.completedQuestionCount = completedQuestionCount;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

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

    // 新增便利方法
    /**
     * 移动到下一个位置
     */
    public void moveToNextPosition() {
        this.queuePosition = (this.queuePosition == null) ? 1 : this.queuePosition + 1;
    }

    /**
     * 重置队列位置
     */
    public void resetQueuePosition() {
        this.queuePosition = 0;
    }

    /**
     * 检查是否已完成所有题目
     */
    public boolean isCompleted() {
        return expectedQuestionCount != null &&
                completedQuestionCount != null &&
                completedQuestionCount.equals(expectedQuestionCount);
    }

    /**
     * 增加已提问题目数量
     */
    public void incrementAskedQuestionCount() {
        this.askedQuestionCount = (this.askedQuestionCount == null) ? 1 : this.askedQuestionCount + 1;
    }

    /**
     * 增加已完成题目数量
     */
    public void incrementCompletedQuestionCount() {
        this.completedQuestionCount = (this.completedQuestionCount == null) ? 1 : this.completedQuestionCount + 1;
    }

    /**
     * 结束会话
     */
    public void endSession() {
        this.isActive = false;
        this.endedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", userId=" + userId +
                ", mode=" + mode +
                ", expectedQuestionCount=" + expectedQuestionCount +
                ", askedQuestionCount=" + askedQuestionCount +
                ", completedQuestionCount=" + completedQuestionCount +
                ", startedAt=" + startedAt +
                ", endedAt=" + endedAt +
                ", isActive=" + isActive +
                '}';
    }
}