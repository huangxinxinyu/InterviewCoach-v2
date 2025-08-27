package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.config.properties.AIQueueProperties;
import com.xinyu.InterviewCoach_v2.queue.monitor.RedisStreamMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 队列监控控制器
 * 提供简单的HTTP接口查看队列状态
 */
@RestController
@RequestMapping("/api/queue")
@CrossOrigin(origins = "*")
public class QueueMonitorController {

    private static final Logger logger = LoggerFactory.getLogger(QueueMonitorController.class);

    @Autowired
    private RedisStreamMonitor streamMonitor;

    @Autowired
    private AIQueueProperties queueProperties;

    /**
     * 获取队列总体状态
     * GET /api/queue/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            // 基本配置信息
            status.put("enabled", queueProperties.isEnabled());
            status.put("websocketEnabled", queueProperties.getWebsocket().isEnabled());
            status.put("timestamp", new Date());

            if (queueProperties.isEnabled()) {
                // 队列统计信息
                status.putAll(streamMonitor.getQueueStats());

                // Stream信息
                Map<String, Object> streams = new HashMap<>();
                streams.put("requests", streamMonitor.getStreamInfo(queueProperties.getStreams().getRequests()));

                if (queueProperties.getWebsocket().isEnabled()) {
                    streams.put("responses", streamMonitor.getStreamInfo(queueProperties.getStreams().getResponses()));
                }
                status.put("streams", streams);

                // 消费者配置
                Map<String, Object> consumers = new HashMap<>();
                consumers.put("aiConsumerGroup", queueProperties.getConsumer().getGroupName());
                consumers.put("aiConsumerName", queueProperties.getConsumer().getConsumerName());

                if (queueProperties.getWebsocket().isEnabled()) {
                    consumers.put("wsConsumerGroup", queueProperties.getWebsocket().getConsumer().getGroupName());
                    consumers.put("wsConsumerName", queueProperties.getWebsocket().getConsumer().getConsumerName());
                }
                status.put("consumers", consumers);
            }

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("获取队列状态失败", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", new Date()
            ));
        }
    }

    /**
     * 获取简化的队列健康检查
     * GET /api/admin/queue/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getQueueHealth() {
        try {
            Map<String, Object> health = new HashMap<>();

            if (!queueProperties.isEnabled()) {
                health.put("status", "DISABLED");
                health.put("message", "队列功能未启用");
                return ResponseEntity.ok(health);
            }

            Map<String, Object> stats = streamMonitor.getQueueStats();

            // 判断健康状态
            String status = "HEALTHY";
            List<String> warnings = new ArrayList<>();

            // 检查请求队列积压
            Object requestsLength = stats.get("requestsQueueLength");
            if (requestsLength instanceof Long && (Long) requestsLength > 50) {
                status = "WARNING";
                warnings.add("请求队列积压: " + requestsLength);
            }

            // 检查响应队列积压
            Object responsesLength = stats.get("responsesQueueLength");
            if (responsesLength instanceof Long && (Long) responsesLength > 50) {
                status = "WARNING";
                warnings.add("响应队列积压: " + responsesLength);
            }

            health.put("status", status);
            health.put("requestsQueueLength", requestsLength);
            health.put("responsesQueueLength", responsesLength);
            health.put("warnings", warnings);
            health.put("timestamp", new Date());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("队列健康检查失败", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "error", e.getMessage(),
                    "timestamp", new Date()
            ));
        }
    }

    /**
     * 手动触发队列检查
     * POST /api/admin/queue/check
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> triggerManualCheck() {
        try {
            if (!queueProperties.isEnabled()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "队列功能未启用"
                ));
            }

            streamMonitor.triggerManualCheck();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "手动检查已触发",
                    "timestamp", new Date()
            ));

        } catch (Exception e) {
            logger.error("触发手动检查失败", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "timestamp", new Date()
            ));
        }
    }

    /**
     * 重新处理挂起消息
     * POST /api/admin/queue/reprocess-pending
     */
    @PostMapping("/reprocess-pending")
    public ResponseEntity<Map<String, Object>> reprocessPendingMessages() {
        try {
            if (!queueProperties.isEnabled()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "队列功能未启用"
                ));
            }

            String requestsStream = queueProperties.getStreams().getRequests();
            String aiGroupName = queueProperties.getConsumer().getGroupName();

            streamMonitor.reprocessPendingMessages(requestsStream, aiGroupName);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "挂起消息重处理已触发",
                    "stream", requestsStream,
                    "group", aiGroupName,
                    "timestamp", new Date()
            ));

        } catch (Exception e) {
            logger.error("重处理挂起消息失败", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "timestamp", new Date()
            ));
        }
    }

    /**
     * 重置消费者组（谨慎使用）
     * POST /api/admin/queue/reset-consumer-group
     */
    @PostMapping("/reset-consumer-group")
    public ResponseEntity<Map<String, Object>> resetConsumerGroup(
            @RequestParam(defaultValue = "ai") String type) {
        try {
            if (!queueProperties.isEnabled()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "队列功能未启用"
                ));
            }

            String streamName;
            String groupName;

            if ("ai".equals(type)) {
                streamName = queueProperties.getStreams().getRequests();
                groupName = queueProperties.getConsumer().getGroupName();
            } else if ("websocket".equals(type)) {
                if (!queueProperties.getWebsocket().isEnabled()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "WebSocket队列未启用"
                    ));
                }
                streamName = queueProperties.getStreams().getResponses();
                groupName = queueProperties.getWebsocket().getConsumer().getGroupName();
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "无效的类型，支持: ai, websocket"
                ));
            }

            boolean success = streamMonitor.resetConsumerGroup(streamName, groupName);

            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "消费者组重置成功" : "消费者组重置失败",
                    "type", type,
                    "stream", streamName,
                    "group", groupName,
                    "timestamp", new Date()
            ));

        } catch (Exception e) {
            logger.error("重置消费者组失败", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "timestamp", new Date()
            ));
        }
    }

    /**
     * 获取配置信息
     * GET /api/admin/queue/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getQueueConfig() {
        try {
            Map<String, Object> config = new HashMap<>();

            config.put("enabled", queueProperties.isEnabled());

            // Stream配置
            Map<String, Object> streams = new HashMap<>();
            streams.put("requests", queueProperties.getStreams().getRequests());
            streams.put("responses", queueProperties.getStreams().getResponses());
            config.put("streams", streams);

            // 消费者配置
            Map<String, Object> consumer = new HashMap<>();
            consumer.put("groupName", queueProperties.getConsumer().getGroupName());
            consumer.put("consumerName", queueProperties.getConsumer().getConsumerName());
            consumer.put("maxMessages", queueProperties.getConsumer().getMaxMessages());
            consumer.put("blockTimeout", queueProperties.getConsumer().getBlockTimeout());
            config.put("aiConsumer", consumer);

            // WebSocket配置
            if (queueProperties.getWebsocket().isEnabled()) {
                Map<String, Object> wsConsumer = new HashMap<>();
                wsConsumer.put("groupName", queueProperties.getWebsocket().getConsumer().getGroupName());
                wsConsumer.put("consumerName", queueProperties.getWebsocket().getConsumer().getConsumerName());
                wsConsumer.put("maxMessages", queueProperties.getWebsocket().getConsumer().getMaxMessages());
                wsConsumer.put("blockTimeout", queueProperties.getWebsocket().getConsumer().getBlockTimeout());
                config.put("websocketConsumer", wsConsumer);
            }

            // 处理器配置
            Map<String, Object> processors = new HashMap<>();
            processors.put("maxRetries", queueProperties.getProcessors().getMaxRetries());
            config.put("processors", processors);

            config.put("timestamp", new Date());

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            logger.error("获取队列配置失败", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", new Date()
            ));
        }
    }
}