package com.xinyu.InterviewCoach_v2.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 用户答题尝试数据传输对象
 */
public class UserAttemptDTO {

    private Long userId;
    private Long questionId;
    private Integer attemptNumber;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // 额外字段
    private String userEmail;        // 用户邮箱
    private String questionText;     // 题目内容
    private Boolean isFirstAttempt;  // 是否为首次尝试

    public UserAttemptDTO() {}

    public UserAttemptDTO(Long userId, Long questionId, Integer attemptNumber, LocalDateTime updatedAt) {
        this.userId = userId;
        this.questionId = questionId;
        this.attemptNumber = attemptNumber;
        this.updatedAt = updatedAt;
    }

    /**
     * 检查是否为首次尝试
     */
    public boolean isFirstAttempt() {
        return attemptNumber != null && attemptNumber == 1;
    }

    /**
     * 获取尝试次数的描述
     */
    public String getAttemptDescription() {
        if (attemptNumber == null || attemptNumber <= 0) {
            return "未尝试";
        } else if (attemptNumber == 1) {
            return "首次尝试";
        } else {
            return "第" + attemptNumber + "次尝试";
        }
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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Boolean getIsFirstAttempt() {
        return isFirstAttempt;
    }

    public void setIsFirstAttempt(Boolean isFirstAttempt) {
        this.isFirstAttempt = isFirstAttempt;
    }

    @Override
    public String toString() {
        return "UserAttemptDTO{" +
                "userId=" + userId +
                ", questionId=" + questionId +
                ", attemptNumber=" + attemptNumber +
                ", updatedAt=" + updatedAt +
                ", userEmail='" + userEmail + '\'' +
                ", questionText='" + (questionText != null && questionText.length() > 50 ? questionText.substring(0, 50) + "..." : questionText) + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserAttemptDTO that = (UserAttemptDTO) o;
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