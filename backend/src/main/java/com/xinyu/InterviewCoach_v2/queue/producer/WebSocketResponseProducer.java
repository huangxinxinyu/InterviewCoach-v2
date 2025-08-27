package com.xinyu.InterviewCoach_v2.queue.producer;

import com.xinyu.InterviewCoach_v2.config.properties.AIQueueProperties;
import com.xinyu.InterviewCoach_v2.dto.queue.AIQueueMessage;
import com.xinyu.InterviewCoach_v2.queue.constants.AIQueueTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WebSocket响应生产者
 * 负责将WebSocket推送请求发送到响应队列
 * 解耦AI处理和WebSocket推送逻辑
 */
@Component
public class WebSocketResponseProducer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketResponseProducer.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AIQueueProperties queueProperties;

    /**
     * 发送AI响应推送请求
     * @param sessionId 会话ID
     * @param message AI响应内容
     * @param currentState 会话当前状态
     */
    @Async("websocketProducerExecutor")
    public void sendAIResponseMessage(Long sessionId, String message, String currentState) {
        if (!isWebSocketQueueEnabled()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "sessionId", sessionId,
                "message", message,
                "currentState", currentState,
                "chatInputEnabled", !"AI_PROCESSING".equals(currentState),
                // 修正：使用常量作为消息类型，而非硬编码字符串
                "type", AIQueueTopics.WEBSOCKET_AI_RESPONSE
        );

        sendMessage(AIQueueTopics.WEBSOCKET_AI_RESPONSE, payload, AIQueueTopics.PRIORITY_HIGH);
        logger.debug("发送AI响应推送请求: sessionId={}, state={}", sessionId, currentState);
    }

    /**
     * 发送AI处理状态推送请求
     * @param sessionId 会话ID
     * @param status 处理状态
     * @param progress 状态详情
     */
    @Async("websocketProducerExecutor")
    public void sendProcessingStatusMessage(Long sessionId, String status, String progress) {
        if (!isWebSocketQueueEnabled()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "sessionId", sessionId,
                "status", status,
                "progress", progress != null ? progress : "",
                // 修正：使用常量作为消息类型，而非硬编码字符串
                "type", AIQueueTopics.WEBSOCKET_PROCESSING_STATUS
        );

        // 修正：使用常量作为消息主题，而非硬编码字符串
        sendMessage(AIQueueTopics.WEBSOCKET_PROCESSING_STATUS, payload, AIQueueTopics.PRIORITY_MEDIUM);
        logger.debug("发送AI处理状态推送请求: sessionId={}, status={}", sessionId, status);
    }

    /**
     * 发送会话状态更新推送请求
     * @param sessionId 会话ID
     * @param currentState 会话状态
     * @param chatInputEnabled 聊天输入是否启用
     */
    @Async("websocketProducerExecutor")
    public void sendSessionStateMessage(Long sessionId, String currentState, boolean chatInputEnabled) {
        if (!isWebSocketQueueEnabled()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "sessionId", sessionId,
                "currentState", currentState,
                "chatInputEnabled", chatInputEnabled,
                // 修正：使用常量作为消息类型，而非硬编码字符串
                "type", AIQueueTopics.WEBSOCKET_SESSION_STATE
        );

        // 修正：使用常量作为消息主题，而非硬编码字符串
        sendMessage(AIQueueTopics.WEBSOCKET_SESSION_STATE, payload, AIQueueTopics.PRIORITY_MEDIUM);
        logger.debug("发送会话状态更新推送请求: sessionId={}, state={}, chatEnabled={}",
                sessionId, currentState, chatInputEnabled);
    }

    /**
     * 发送用户通知推送请求
     * @param userId 用户ID
     * @param message 通知内容
     * @param notificationType 通知类型
     */
    @Async("websocketProducerExecutor")
    public void sendUserNotificationMessage(Long userId, String message, String notificationType) {
        if (!isWebSocketQueueEnabled()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "userId", userId,
                "message", message,
                "notificationType", notificationType,
                // 修正：使用常量作为消息类型，而非硬编码字符串
                "type", AIQueueTopics.WEBSOCKET_USER_NOTIFICATION
        );

        // 修正：使用常量作为消息主题，而非硬编码字符串
        sendMessage(AIQueueTopics.WEBSOCKET_USER_NOTIFICATION, payload, AIQueueTopics.PRIORITY_LOW);
        logger.debug("发送用户通知推送请求: userId={}, type={}", userId, notificationType);
    }

    /**
     * 通用消息发送方法
     * @param topic 消息主题
     * @param payload 消息载荷
     * @param priority 消息优先级
     */
    @Async("websocketProducerExecutor")
    protected void sendMessage(String topic, Map<String, Object> payload, String priority) {
        try {
            AIQueueMessage message = AIQueueMessage.create(topic, payload, priority);

            Map<String, Object> streamRecord = Map.of(
                    "messageId", message.getMessageId(),
                    "topic", message.getTopic(),
                    "payload", objectMapper.writeValueAsString(message.getPayload()),
                    "priority", message.getPriority(),
                    "retryCount", message.getRetryCount(),
                    "timestamp", message.getTimestamp().toString()
            );

            // 发送到响应队列
            redisTemplate.opsForStream().add(queueProperties.getStreams().getResponses(), streamRecord);

        } catch (Exception e) {
            logger.error("发送WebSocket响应消息到队列失败: topic={}", topic, e);
            throw new RuntimeException("WebSocket响应队列操作失败", e);
        }
    }

    /**
     * 检查WebSocket队列是否启用
     */
    private boolean isWebSocketQueueEnabled() {
        return queueProperties.isEnabled() && queueProperties.getWebsocket().isEnabled();
    }
}