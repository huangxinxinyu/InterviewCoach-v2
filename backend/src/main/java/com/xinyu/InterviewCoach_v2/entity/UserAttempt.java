package com.xinyu.InterviewCoach_v2.entity;

import java.time.LocalDateTime;

/**
 * 用户题目尝试次数实体类
 */
public class UserAttempt {

    private Long userId;
    private Long questionId;
    private Integer attemptNumber;
    private LocalDateTime updatedAt;

    public UserAttempt() {
        this.attemptNumber = 1;
        this.updatedAt = LocalDateTime.now();
    }

    public UserAttempt(Long userId, Long questionId) {
        this();
        this.userId = userId;
        this.questionId = questionId;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 增加尝试次数
     */
    public void incrementAttempt() {
        this.attemptNumber++;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "UserAttempt{" +
                "userId=" + userId +
                ", questionId=" + questionId +
                ", attemptNumber=" + attemptNumber +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserAttempt that = (UserAttempt) o;
        return userId != null && userId.equals(that.userId) &&
                questionId != null && questionId.equals(that.questionId);
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (questionId != null ? questionId.hashCode() : 0);
        return result;
    }
}