package com.xinyu.InterviewCoach_v2.enums;

/**
 * 消息类型枚举
 */
public enum MessageType {
    AI("AI", "AI消息"),
    USER("USER", "用户消息");

    private final String value;
    private final String description;

    MessageType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static MessageType fromValue(String value) {
        for (MessageType type : MessageType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MessageType value: " + value);
    }
}

