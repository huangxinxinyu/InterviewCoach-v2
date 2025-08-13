package com.xinyu.InterviewCoach_v2.dto.request.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发送消息请求DTO
 */
public class SendMessageRequestDTO {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容长度不能超过5000个字符")
    private String text;

    public SendMessageRequestDTO() {}

    public SendMessageRequestDTO(String text) {
        this.text = text;
    }

    /**
     * 获取处理后的文本（去除首尾空格）
     */
    public String getProcessedText() {
        return text != null ? text.trim() : null;
    }

    /**
     * 检查消息是否有效
     */
    public boolean isValid() {
        return text != null && !text.trim().isEmpty();
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "SendMessageRequestDTO{" +
                "text='" + (text != null ? text.substring(0, Math.min(text.length(), 50)) + "..." : null) + '\'' +
                '}';
    }
}