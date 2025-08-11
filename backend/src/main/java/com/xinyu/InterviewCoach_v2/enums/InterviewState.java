package com.xinyu.InterviewCoach_v2.enums;

/**
 * 面试状态枚举
 */
public enum InterviewState {
    /**
     * 面试开始 - 等待AI发出第一个问题
     */
    STARTED("面试开始"),

    /**
     * 等待用户回答
     */
    WAITING_FOR_USER_ANSWER("等待用户回答"),

    /**
     * AI正在分析用户回答并准备反馈
     */
    AI_ANALYZING("AI分析中"),

    /**
     * AI给出反馈并准备下一题
     */
    AI_FEEDBACK("AI反馈中"),

    /**
     * 面试结束 - AI进行整体评价
     */
    INTERVIEW_COMPLETED("面试结束"),

    /**
     * 会话结束
     */
    SESSION_ENDED("会话结束");

    private final String description;

    InterviewState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
