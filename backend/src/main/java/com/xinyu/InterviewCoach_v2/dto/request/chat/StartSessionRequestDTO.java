package com.xinyu.InterviewCoach_v2.dto.request.chat;

import com.xinyu.InterviewCoach_v2.enums.SessionMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * 开始面试会话请求DTO
 */
public class StartSessionRequestDTO {

    @NotNull(message = "会话模式不能为空")
    private SessionMode mode;

    @NotNull(message = "期望题目数量不能为空")
    @Min(value = 1, message = "期望题目数量至少为1")
    @Max(value = 10, message = "期望题目数量最多为10")
    private Integer expectedQuestionCount;

    private Long tagId; // 用于单主题模式
    private List<Long> questionIds; // 用于结构化题集模式

    public StartSessionRequestDTO() {}

    public StartSessionRequestDTO(SessionMode mode, Integer expectedQuestionCount) {
        this.mode = mode;
        this.expectedQuestionCount = expectedQuestionCount;
    }

    public StartSessionRequestDTO(SessionMode mode, Integer expectedQuestionCount, Long tagId) {
        this.mode = mode;
        this.expectedQuestionCount = expectedQuestionCount;
        this.tagId = tagId;
    }

    public StartSessionRequestDTO(SessionMode mode, Integer expectedQuestionCount, List<Long> questionIds) {
        this.mode = mode;
        this.expectedQuestionCount = expectedQuestionCount;
        this.questionIds = questionIds;
    }

    /**
     * 验证请求参数
     */
    public boolean isValid() {
        if (mode == null || expectedQuestionCount == null) {
            return false;
        }

        switch (mode) {
            case SINGLE_TOPIC:
                return tagId != null;
            case STRUCTURED_SET:
                return questionIds != null && !questionIds.isEmpty()
                        && questionIds.size() >= expectedQuestionCount;
            case STRUCTURED_TEMPLATE:
                return true; // 模板模式不需要额外参数
            default:
                return false;
        }
    }

    // Getters and Setters
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

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public List<Long> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<Long> questionIds) {
        this.questionIds = questionIds;
    }

    @Override
    public String toString() {
        return "StartSessionRequestDTO{" +
                "mode=" + mode +
                ", expectedQuestionCount=" + expectedQuestionCount +
                ", tagId=" + tagId +
                ", questionIds=" + questionIds +
                '}';
    }
}