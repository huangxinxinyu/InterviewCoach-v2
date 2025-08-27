package com.xinyu.InterviewCoach_v2.queue.monitor;

import com.xinyu.InterviewCoach_v2.config.properties.AIQueueProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * 简化版Redis Stream监控组件
 * 使用最新的Spring Data Redis API
 */
@Component
public class RedisStreamMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RedisStreamMonitor.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AIQueueProperties queueProperties;

    /**
     * 监控队列长度 - 每分钟检查一次
     */
    @Scheduled(fixedDelay = 60000)
    @Async("streamMonitorExecutor")
    public void monitorQueueLength() {
        if (!queueProperties.isEnabled()) {
            return;
        }

        try {
            String requestsStream = queueProperties.getStreams().getRequests();
            String responsesStream = queueProperties.getStreams().getResponses();

            // 检查请求队列长度
            Long requestsLength = redisTemplate.opsForStream().size(requestsStream);
            if (requestsLength != null && requestsLength > 100) {
                logger.warn("AI请求队列积压: stream={}, length={}", requestsStream, requestsLength);
            }

            // 检查响应队列长度
            if (queueProperties.getWebsocket().isEnabled()) {
                Long responsesLength = redisTemplate.opsForStream().size(responsesStream);
                if (responsesLength != null && responsesLength > 100) {
                    logger.warn("WebSocket响应队列积压: stream={}, length={}", responsesStream, responsesLength);
                }
            }

            logger.debug("队列监控: requests={}, responses={}", requestsLength,
                    queueProperties.getWebsocket().isEnabled() ? redisTemplate.opsForStream().size(responsesStream) : "disabled");

        } catch (Exception e) {
            logger.error("监控队列长度失败", e);
        }
    }

    /**
     * 清理过期消息 - 每小时执行一次
     */
    @Scheduled(fixedDelay = 3600000)
    @Async("streamMonitorExecutor")
    public void cleanupExpiredMessages() {
        if (!queueProperties.isEnabled()) {
            return;
        }

        try {
            String requestsStream = queueProperties.getStreams().getRequests();
            String responsesStream = queueProperties.getStreams().getResponses();

            // 清理6小时前的消息
            String cutoffId = generateTimeBasedId(System.currentTimeMillis() - 6 * 60 * 60 * 1000);

            // 清理请求流
            Long deletedRequests = redisTemplate.opsForStream().trim(requestsStream, 1000);
            if (deletedRequests != null && deletedRequests > 0) {
                logger.info("清理过期请求消息: stream={}, deleted={}", requestsStream, deletedRequests);
            }

            // 清理响应流
            if (queueProperties.getWebsocket().isEnabled()) {
                Long deletedResponses = redisTemplate.opsForStream().trim(responsesStream, 500);
                if (deletedResponses != null && deletedResponses > 0) {
                    logger.info("清理过期响应消息: stream={}, deleted={}", responsesStream, deletedResponses);
                }
            }

        } catch (Exception e) {
            logger.error("清理过期消息失败", e);
        }
    }

    /**
     * 检查消费者组状态 - 每5分钟检查一次
     */
    @Scheduled(fixedDelay = 300000)
    @Async("streamMonitorExecutor")
    public void checkConsumerGroups() {
        if (!queueProperties.isEnabled()) {
            return;
        }

        try {
            String requestsStream = queueProperties.getStreams().getRequests();
            String aiGroupName = queueProperties.getConsumer().getGroupName();

            // 检查AI请求消费者组是否存在
            if (!isConsumerGroupExists(requestsStream, aiGroupName)) {
                logger.warn("AI消费者组不存在，尝试重新创建: stream={}, group={}", requestsStream, aiGroupName);
                try {
                    redisTemplate.opsForStream().createGroup(requestsStream, aiGroupName);
                    logger.info("AI消费者组创建成功: stream={}, group={}", requestsStream, aiGroupName);
                } catch (Exception e) {
                    logger.error("创建AI消费者组失败", e);
                }
            }

            // 检查WebSocket响应消费者组
            if (queueProperties.getWebsocket().isEnabled()) {
                String responsesStream = queueProperties.getStreams().getResponses();
                String wsGroupName = queueProperties.getWebsocket().getConsumer().getGroupName();

                if (!isConsumerGroupExists(responsesStream, wsGroupName)) {
                    logger.warn("WebSocket消费者组不存在，尝试重新创建: stream={}, group={}", responsesStream, wsGroupName);
                    try {
                        redisTemplate.opsForStream().createGroup(responsesStream, wsGroupName);
                        logger.info("WebSocket消费者组创建成功: stream={}, group={}", responsesStream, wsGroupName);
                    } catch (Exception e) {
                        logger.error("创建WebSocket消费者组失败", e);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("检查消费者组状态失败", e);
        }
    }

    /**
     * 手动重新处理挂起消息
     */
    public void reprocessPendingMessages(String streamName, String groupName) {
        try {
            String consumerName = "recovery-consumer-" + System.currentTimeMillis();

            // 使用XPENDING获取挂起消息
            List<MapRecord<String, Object, Object>> pendingMessages = redisTemplate.opsForStream()
                    .read(Consumer.from(groupName, consumerName),
                            StreamReadOptions.empty().count(10),
                            StreamOffset.create(streamName, ReadOffset.from("0")));

            if (pendingMessages != null && !pendingMessages.isEmpty()) {
                logger.info("发现挂起消息: stream={}, group={}, count={}",
                        streamName, groupName, pendingMessages.size());

                for (MapRecord<String, Object, Object> message : pendingMessages) {
                    try {
                        // 确认消息，让其可以被重新处理
                        redisTemplate.opsForStream().acknowledge(streamName, groupName, message.getId());
                        logger.debug("确认挂起消息: messageId={}", message.getId());
                    } catch (Exception e) {
                        logger.error("确认挂起消息失败: messageId={}", message.getId(), e);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("重新处理挂起消息失败: stream={}, group={}", streamName, groupName, e);
        }
    }

    /**
     * 获取队列统计信息
     */
    public Map<String, Object> getQueueStats() {
        Map<String, Object> stats = new HashMap<>();

        if (!queueProperties.isEnabled()) {
            stats.put("enabled", false);
            return stats;
        }

        try {
            String requestsStream = queueProperties.getStreams().getRequests();
            String responsesStream = queueProperties.getStreams().getResponses();

            // 请求队列统计
            Long requestsLength = redisTemplate.opsForStream().size(requestsStream);
            stats.put("requestsQueueLength", requestsLength != null ? requestsLength : 0);

            // 响应队列统计
            if (queueProperties.getWebsocket().isEnabled()) {
                Long responsesLength = redisTemplate.opsForStream().size(responsesStream);
                stats.put("responsesQueueLength", responsesLength != null ? responsesLength : 0);
            } else {
                stats.put("responsesQueueLength", "disabled");
            }

            stats.put("enabled", true);
            stats.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            logger.error("获取队列统计信息失败", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * 检查消费者组是否存在
     */
    private boolean isConsumerGroupExists(String streamName, String groupName) {
        try {
            // 尝试读取消费者组信息
            redisTemplate.opsForStream().read(Consumer.from(groupName, "test-consumer"),
                    StreamReadOptions.empty().count(1).block(Duration.ofMillis(1)),
                    StreamOffset.create(streamName, ReadOffset.lastConsumed()));
            return true;
        } catch (Exception e) {
            // 如果出现异常，可能是消费者组不存在
            return false;
        }
    }

    /**
     * 生成基于时间的消息ID（简化版）
     */
    private String generateTimeBasedId(long timestamp) {
        return timestamp + "-0";
    }

    /**
     * 获取指定Stream的基本信息
     */
    public Map<String, Object> getStreamInfo(String streamName) {
        Map<String, Object> info = new HashMap<>();

        try {
            Long length = redisTemplate.opsForStream().size(streamName);
            info.put("length", length != null ? length : 0);
            info.put("exists", redisTemplate.hasKey(streamName));
            info.put("streamName", streamName);

        } catch (Exception e) {
            logger.error("获取Stream信息失败: {}", streamName, e);
            info.put("error", e.getMessage());
        }

        return info;
    }

    /**
     * 手动触发队列检查
     */
    public void triggerManualCheck() {
        logger.info("手动触发队列检查...");

        try {
            monitorQueueLength();
            checkConsumerGroups();
            logger.info("手动队列检查完成");
        } catch (Exception e) {
            logger.error("手动队列检查失败", e);
        }
    }

    /**
     * 重置消费者组（谨慎使用）
     */
    public boolean resetConsumerGroup(String streamName, String groupName) {
        try {
            // 删除现有消费者组
            redisTemplate.opsForStream().destroyGroup(streamName, groupName);
            logger.warn("已删除消费者组: stream={}, group={}", streamName, groupName);

            // 重新创建消费者组
            redisTemplate.opsForStream().createGroup(streamName, groupName);
            logger.info("重新创建消费者组: stream={}, group={}", streamName, groupName);

            return true;

        } catch (Exception e) {
            logger.error("重置消费者组失败: stream={}, group={}", streamName, groupName, e);
            return false;
        }
    }
}