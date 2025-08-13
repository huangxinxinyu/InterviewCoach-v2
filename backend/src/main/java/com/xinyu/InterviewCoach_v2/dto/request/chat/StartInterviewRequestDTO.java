package com.xinyu.InterviewCoach_v2.dto.request.chat;

import com.xinyu.InterviewCoach_v2.enums.SessionMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;

/**
 * 开始面试请求DTO - 添加模板支持
 */
public class StartInterviewRequestDTO {

    @NotNull(message = "会话模式不能为空")
    private SessionMode mode;

    // 对于 structured_template 和 structured_set 模式，此字段可为空，将自动计算
    // 对于其他模式，此字段必填
    @Min(value = 1, message = "期望题目数量至少为1")
    @Max(value = 10, message = "期望题目数量最多为10")
    private Integer expectedQuestionCount;

    private Long tagId; // 用于单主题模式
    private List<Long> questionIds; // 用于结构化题集模式
    private Long questionSetId; // 用于结构化题集模式（替代 questionIds）
    private Long templateId; // 用于结构化模板模式

    public StartInterviewRequestDTO() {}

    public StartInterviewRequestDTO(SessionMode mode, Integer expectedQuestionCount) {
        this.mode = mode;
        this.expectedQuestionCount = expectedQuestionCount;
    }

    public StartInterviewRequestDTO(SessionMode mode, Integer expectedQuestionCount, Long tagId) {
        this.mode = mode;
        this.expectedQuestionCount = expectedQuestionCount;
        this.tagId = tagId;
    }

    // 用于 structured_set 模式的构造方法
    public StartInterviewRequestDTO(SessionMode mode, Long questionSetId) {
        this.mode = mode;
        this.questionSetId = questionSetId;
    }

    // 用于 structured_template 模式的构造方法
    public StartInterviewRequestDTO(SessionMode mode, Long templateId, boolean isTemplate) {
        this.mode = mode;
        this.templateId = templateId;
    }

    // 兼容原有的构造方法
    public StartInterviewRequestDTO(SessionMode mode, Integer expectedQuestionCount, List<Long> questionIds) {
        this.mode = mode;
        this.expectedQuestionCount = expectedQuestionCount;
        this.questionIds = questionIds;
    }

    /**
     * 验证请求参数
     */
    public boolean isValid() {
        if (mode == null) {
            return false;
        }

        switch (mode) {
            case SINGLE_TOPIC:
                return tagId != null && expectedQuestionCount != null;
            case STRUCTURED_SET:
                // structured_set 模式可以使用 questionSetId 或 questionIds
                return questionSetId != null ||
                        (questionIds != null && !questionIds.isEmpty());
            case STRUCTURED_TEMPLATE:
                // structured_template 模式只需要 templateId
                return templateId != null;
            default:
                return expectedQuestionCount != null;
        }
    }

    /**
     * 检查是否为模板模式
     */
    public boolean isTemplateMode() {
        return mode == SessionMode.STRUCTURED_TEMPLATE && templateId != null;
    }

    /**
     * 检查是否为题集模式且使用题集ID
     */
    public boolean isQuestionSetMode() {
        return mode == SessionMode.STRUCTURED_SET && questionSetId != null;
    }

    /**
     * 检查是否为题集模式且使用题目ID列表
     */
    public boolean isQuestionIdsMode() {
        return mode == SessionMode.STRUCTURED_SET &&
                questionIds != null && !questionIds.isEmpty();
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

    public Long getQuestionSetId() {
        return questionSetId;
    }

    public void setQuestionSetId(Long questionSetId) {
        this.questionSetId = questionSetId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    @Override
    public String toString() {
        return "StartInterviewRequestDTO{" +
                "mode=" + mode +
                ", expectedQuestionCount=" + expectedQuestionCount +
                ", tagId=" + tagId +
                ", questionIds=" + questionIds +
                ", questionSetId=" + questionSetId +
                ", templateId=" + templateId +
                '}';
    }
}