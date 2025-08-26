package com.xinyu.InterviewCoach_v2.dto.queue;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * AI队列消息实体
 * 用于在Redis Stream中传递AI处理请求
 */
public class AIQueueMessage {

    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 消息主题（如: ai.feedback_generation, ai.question_generation等）
     */
    private String topic;

    /**
     * 消息载荷数据
     */
    private Map<String, Object> payload;

    /**
     * 消息优先级（high, medium, low）
     */
    private String priority;

    /**
     * 重试次数
     */
    private int retryCount = 0;

    /**
     * 消息创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();

    // 构造方法
    public AIQueueMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public AIQueueMessage(String topic, Map<String, Object> payload, String priority) {
        this();
        this.messageId = UUID.randomUUID().toString();
        this.topic = topic;
        this.payload = payload;
        this.priority = priority;
        this.retryCount = 0;
    }

    /**
     * 创建AI队列消息的工厂方法
     */
    public static AIQueueMessage create(String topic, Map<String, Object> payload, String priority) {
        return new AIQueueMessage(topic, payload, priority);
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * 检查是否超过最大重试次数
     */
    public boolean isMaxRetriesExceeded(int maxRetries) {
        return this.retryCount >= maxRetries;
    }

    /**
     * 获取消息年龄（毫秒）
     */
    public long getAgeInMillis() {
        return java.time.Duration.between(timestamp, LocalDateTime.now()).toMillis();
    }

    /**
     * 检查消息是否过期
     */
    public boolean isExpired(long maxAgeMillis) {
        return getAgeInMillis() > maxAgeMillis;
    }

    // Getter和Setter方法
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}