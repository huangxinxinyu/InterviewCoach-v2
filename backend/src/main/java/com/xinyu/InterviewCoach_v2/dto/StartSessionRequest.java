package com.xinyu.InterviewCoach_v2.dto;

import com.xinyu.InterviewCoach_v2.enums.SessionMode;

import java.util.List;

/**
 * 开始会话请求DTO
 */
public class StartSessionRequest {
    private SessionMode mode;
    private Integer expectedQuestionCount; // 用于单主题模式和结构化模板模式
    private Long tagId; // 用于单主题模式
    private Long questionSetId; // 用于结构化题集模式
    private List<Long> questionIds; // 保留用于直接指定题目（可选）

    public StartSessionRequest() {}

    // Getters and Setters
    public SessionMode getMode() { return mode; }
    public void setMode(SessionMode mode) { this.mode = mode; }

    public Integer getExpectedQuestionCount() { return expectedQuestionCount; }
    public void setExpectedQuestionCount(Integer expectedQuestionCount) { this.expectedQuestionCount = expectedQuestionCount; }

    public Long getTagId() { return tagId; }
    public void setTagId(Long tagId) { this.tagId = tagId; }

    public Long getQuestionSetId() { return questionSetId; }
    public void setQuestionSetId(Long questionSetId) { this.questionSetId = questionSetId; }

    public List<Long> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<Long> questionIds) { this.questionIds = questionIds; }
}