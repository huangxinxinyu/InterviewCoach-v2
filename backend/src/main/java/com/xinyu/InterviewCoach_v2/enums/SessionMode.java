package com.xinyu.InterviewCoach_v2.enums;

/**
 * 面试会话模式枚举
 */
public enum SessionMode {
    /**
     * 结构化模板模式 - AI根据模板从题库中智能选题
     */
    STRUCTURED_TEMPLATE("structured_template", "结构化模板模式"),

    /**
     * 结构化题集模式 - 使用预设的题集进行面试
     */
    STRUCTURED_SET("structured_set", "结构化题集模式"),

    /**
     * 单主题模式 - 选择一个tag下的题目进行面试
     */
    SINGLE_TOPIC("single_topic", "单主题模式");

    private final String value;
    private final String description;

    SessionMode(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据值获取枚举
     */
    public static SessionMode fromValue(String value) {
        for (SessionMode mode : SessionMode.values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown SessionMode value: " + value);
    }
}