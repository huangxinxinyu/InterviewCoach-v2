package com.xinyu.InterviewCoach_v2.dto;

import java.time.LocalDateTime;

/**
 * 题目数据传输对象 - 用于请求和响应
 */
public class QuestionDTO {

    private Long id;
    private String text;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public QuestionDTO() {}

    public QuestionDTO(String text) {
        this.text = text;
    }

    public QuestionDTO(Long id, String text, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.text = text;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "QuestionDTO{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QuestionDTO that = (QuestionDTO) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * 验证请求数据的有效性
     */
    public boolean isValid() {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * 获取处理后的题目文本（去除首尾空格）
     */
    public String getProcessedText() {
        return text != null ? text.trim() : null;
    }
}