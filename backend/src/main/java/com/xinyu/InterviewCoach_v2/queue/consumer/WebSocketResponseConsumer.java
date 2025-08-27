package com.xinyu.InterviewCoach_v2.queue.consumer;

import com.xinyu.InterviewCoach_v2.queue.constants.AIQueueTopics;
import com.xinyu.InterviewCoach_v2.service.WebSocketService;
import com.xinyu.InterviewCoach_v2.config.properties.AIQueueProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * WebSocket响应消费者
 * 专门负责处理WebSocket消息推送队列
 * 与AI处理逻辑完全解耦
 */
@Component
public class WebSocketResponseConsumer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketResponseConsumer.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private AIQueueProperties queueProperties;

    @PostConstruct
    public void initialize() {
        if (!queueProperties.isEnabled()) {
            logger.info("AI队列未启用，跳过WebSocket响应消费者初始化");
            return;
        }

        try {
            String streamName = queueProperties.getStreams().getResponses();
            String groupName = "websocket-response-group";

            redisTemplate.opsForStream().createGroup(streamName, groupName);
            logger.info("WebSocket响应消费者组初始化成功: stream={}, group={}", streamName, groupName);

        } catch (Exception e) {
            logger.debug("WebSocket响应消费者组可能已存在: {}", e.getMessage());
        }
    }

    /**
     * 定时轮询WebSocket响应队列 - 高优先级消息
     */
    @Scheduled(fixedDelay = 200)
    @Async("taskExecutor")
    public void pollHighPriorityResponses() {
        if (!queueProperties.isEnabled()) {
            return;
        }
        pollWebSocketResponsesByPriority(AIQueueTopics.PRIORITY_HIGH, 5);
    }

    /**
     * 定时轮询WebSocket响应队列 - 中优先级消息
     */
    @Scheduled(fixedDelay = 500)
    @Async("taskExecutor")
    public void pollMediumPriorityResponses() {
        if (!queueProperties.isEnabled()) {
            return;
        }
        pollWebSocketResponsesByPriority(AIQueueTopics.PRIORITY_MEDIUM, 8);
    }

    /**
     * 定时轮询WebSocket响应队列 - 低优先级消息
     */
    @Scheduled(fixedDelay = 1000)
    @Async("taskExecutor")
    public void pollLowPriorityResponses() {
        if (!queueProperties.isEnabled()) {
            return;
        }
        pollWebSocketResponsesByPriority(AIQueueTopics.PRIORITY_LOW, 10);
    }

    /**
     * 按优先级轮询WebSocket响应消息
     */
    private void pollWebSocketResponsesByPriority(String priority, int maxCount) {
        try {
            String streamName = queueProperties.getStreams().getResponses();
            String groupName = "websocket-response-group";
            String consumerName = "ws-consumer-" + Thread.currentThread().getName();

            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .read(Consumer.from(groupName, consumerName),
                            StreamReadOptions.empty().count(maxCount).block(Duration.ofMillis(1000)),
                            StreamOffset.create(streamName, ReadOffset.lastConsumed()));

            if (records != null && !records.isEmpty()) {
                // 按优先级过滤
                List<MapRecord<String, Object, Object>> filteredRecords = records.stream()
                        .filter(record -> priority.equals(record.getValue().get("priority")))
                        .toList();

                if (!filteredRecords.isEmpty()) {
                    logger.debug("收到{}优先级WebSocket响应消息: count={}", priority, filteredRecords.size());

                    for (MapRecord<String, Object, Object> record : filteredRecords) {
                        CompletableFuture.runAsync(() -> processWebSocketMessage(record),
                                CompletableFuture.delayedExecutor(0, java.util.concurrent.TimeUnit.MILLISECONDS));
                    }
                }
            }

        } catch (Exception e) {
            logger.error("轮询{}优先级WebSocket响应队列失败", priority, e);
        }
    }

    /**
     * 处理WebSocket响应消息
     */
    private void processWebSocketMessage(MapRecord<String, Object, Object> record) {
        String messageId = null;
        String topic = null;

        try {
            Map<Object, Object> data = record.getValue();
            messageId = (String) data.get("messageId");
            topic = (String) data.get("topic");
            String payloadStr = (String) data.get("payload");

            Map<String, Object> payload = objectMapper.readValue(payloadStr,
                    new TypeReference<Map<String, Object>>() {});

            logger.debug("开始处理WebSocket消息: topic={}, messageId={}", topic, messageId);
            long startTime = System.currentTimeMillis();

            // 根据topic分发处理WebSocket推送
            boolean success = switch(topic) {
                case AIQueueTopics.WEBSOCKET_AI_RESPONSE -> processAIResponsePush(payload);
                case AIQueueTopics.WEBSOCKET_PROCESSING_STATUS -> processProcessingStatusPush(payload);
                case AIQueueTopics.WEBSOCKET_SESSION_STATE -> processSessionStatePush(payload);
                case AIQueueTopics.WEBSOCKET_USER_NOTIFICATION -> processUserNotificationPush(payload);
                default -> {
                    logger.warn("未知的WebSocket Topic: {}", topic);
                    yield false;
                }
            };

            if (success) {
                // 确认消息处理完成
                acknowledgeMessage(String.valueOf(record.getId()));

                long duration = System.currentTimeMillis() - startTime;
                logger.debug("WebSocket消息处理完成: topic={}, messageId={}, 耗时={}ms",
                        topic, messageId, duration);
            } else {
                // 推送失败，处理重试逻辑
                handlePushFailure(record, "WebSocket推送失败");
            }

        } catch (Exception e) {
            logger.error("处理WebSocket消息失败: topic={}, messageId={}", topic, messageId, e);
            handlePushFailure(record, e.getMessage());
        }
    }

    /**
     * 处理AI响应推送
     */
    private boolean processAIResponsePush(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        String content = (String) payload.get("message");
        String currentState = (String) payload.get("currentState");

        logger.debug("处理AI响应推送: sessionId={}, content={}, currentState={}",
                sessionId, content != null ? "非null" : "null", currentState);

        return webSocketService.pushAIResponse(sessionId, content, currentState);
    }

    /**
     * 处理AI处理状态推送
     */
    private boolean processProcessingStatusPush(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        String status = (String) payload.get("status");
        String details = (String) payload.get("progress");

        return webSocketService.pushAIProcessingStatus(sessionId, status, details);
    }

    /**
     * 处理会话状态更新推送
     */
    private boolean processSessionStatePush(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        String state = (String) payload.get("currentState");
        Boolean chatEnabled = (Boolean) payload.get("chatInputEnabled");

        return webSocketService.pushSessionStateUpdate(sessionId, state,
                chatEnabled != null ? chatEnabled : false);
    }

    /**
     * 处理用户通知推送
     */
    private boolean processUserNotificationPush(Map<String, Object> payload) {
        Long userId = getLongValue(payload, "userId");
        String message = (String) payload.get("message");
        String notificationType = (String) payload.get("notificationType");

        return webSocketService.pushUserNotification(userId, message, notificationType);
    }

    /**
     * 获取Long类型值的工具方法（复用AIQueueConsumer的模式）
     */
    private Long getLongValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                logger.warn("无法解析Long值: key={}, value={}", key, value);
                return null;
            }
        }
        return null;
    }

    /**
     * 确认消息处理完成
     */
    private void acknowledgeMessage(String messageId) {
        try {
            String streamName = queueProperties.getStreams().getResponses();
            String groupName = "websocket-response-group";

            redisTemplate.opsForStream().acknowledge(streamName, groupName, messageId);

        } catch (Exception e) {
            logger.error("确认WebSocket响应消息失败: messageId={}", messageId, e);
        }
    }

    /**
     * 处理推送失败情况
     */
    private void handlePushFailure(MapRecord<String, Object, Object> record, String errorMessage) {
        try {
            Map<Object, Object> data = record.getValue();
            String messageId = (String) data.get("messageId");
            Integer retryCount = (Integer) data.get("retryCount");
            int currentRetryCount = retryCount != null ? retryCount : 0;

            if (currentRetryCount < queueProperties.getProcessors().getMaxRetries()) {
                logger.warn("WebSocket推送失败，准备重试: messageId={}, 当前重试次数={}, 错误={}",
                        messageId, currentRetryCount, errorMessage);
                // 这里可以实现重试机制，但为了简单起见，暂时只记录日志
            } else {
                logger.error("WebSocket推送达到最大重试次数，放弃: messageId={}, 错误={}",
                        messageId, errorMessage);
                // 确认消息以避免重复处理
                acknowledgeMessage(String.valueOf(record.getId()));
            }

        } catch (Exception e) {
            logger.error("处理WebSocket推送失败时出错", e);
        }
    }
}