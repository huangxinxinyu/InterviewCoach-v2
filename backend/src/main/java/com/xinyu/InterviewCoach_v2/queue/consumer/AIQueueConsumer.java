package com.xinyu.InterviewCoach_v2.queue.consumer;

import com.xinyu.InterviewCoach_v2.mapper.QuestionMapper;
import com.xinyu.InterviewCoach_v2.queue.constants.AIQueueTopics;
import com.xinyu.InterviewCoach_v2.queue.producer.WebSocketResponseProducer;
import com.xinyu.InterviewCoach_v2.service.ChatService;
import com.xinyu.InterviewCoach_v2.service.SessionService;
import com.xinyu.InterviewCoach_v2.service.cache.AIResponseCacheManager;
import com.xinyu.InterviewCoach_v2.config.properties.AIQueueProperties;
import com.xinyu.InterviewCoach_v2.entity.Question;
import com.xinyu.InterviewCoach_v2.dto.core.MessageDTO;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class AIQueueConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AIQueueConsumer.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ChatService chatService;

    @Autowired
    private SessionService sessionService;

    // 移除WebSocketService，改为使用WebSocketResponseProducer
    @Autowired
    private WebSocketResponseProducer webSocketResponseProducer;

    @Autowired
    private AIResponseCacheManager aiCacheManager;

    @Autowired
    private AIQueueProperties queueProperties;

    @PostConstruct
    public void initialize() {
        if (!queueProperties.isEnabled()) {
            logger.info("AI队列未启用，跳过消费者初始化");
            return;
        }

        try {
            String streamName = queueProperties.getStreams().getRequests();
            String groupName = queueProperties.getConsumer().getGroupName();

            redisTemplate.opsForStream().createGroup(streamName, groupName);
            logger.info("AI队列消费者组初始化成功: stream={}, group={}", streamName, groupName);

        } catch (Exception e) {
            logger.debug("消费者组可能已存在: {}", e.getMessage());
        }
    }

    /**
     * 定时轮询AI请求队列 - 按优先级处理
     */
    @Scheduled(fixedDelay = 500)
    @Async("aiProcessorExecutor")
    public void pollHighPriorityAIRequests() {
        if (!queueProperties.isEnabled()) {
            return;
        }
        pollAIRequestsByPriority(AIQueueTopics.PRIORITY_HIGH, 3);
    }

    @Scheduled(fixedDelay = 1000)
    @Async("aiProcessorExecutor")
    public void pollMediumPriorityAIRequests() {
        if (!queueProperties.isEnabled()) {
            return;
        }
        pollAIRequestsByPriority(AIQueueTopics.PRIORITY_MEDIUM, 5);
    }

    @Scheduled(fixedDelay = 2000)
    @Async("aiProcessorExecutor")
    public void pollLowPriorityAIRequests() {
        if (!queueProperties.isEnabled()) {
            return;
        }
        pollAIRequestsByPriority(AIQueueTopics.PRIORITY_LOW, 2);
    }

    private void pollAIRequestsByPriority(String priority, int maxCount) {
        try {
            String streamName = queueProperties.getStreams().getRequests();
            String groupName = queueProperties.getConsumer().getGroupName();
            String consumerName = queueProperties.getConsumer().getConsumerName();

            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .read(Consumer.from(groupName, consumerName),
                            StreamReadOptions.empty().count(maxCount).block(Duration.ofMillis(queueProperties.getConsumer().getBlockTimeout())),
                            StreamOffset.create(streamName, ReadOffset.lastConsumed()));

            if (records != null && !records.isEmpty()) {
                // 按优先级过滤
                List<MapRecord<String, Object, Object>> filteredRecords = records.stream()
                        .filter(record -> priority.equals(record.getValue().get("priority")))
                        .toList();

                if (!filteredRecords.isEmpty()) {
                    logger.debug("收到{}优先级AI消息: count={}", priority, filteredRecords.size());

                    for (MapRecord<String, Object, Object> record : filteredRecords) {
                        CompletableFuture.runAsync(() -> processAIMessage(record));
                    }
                }
            }

        } catch (Exception e) {
            logger.error("轮询{}优先级AI队列失败", priority, e);
        }
    }

    /**
     * 处理AI消息
     */
    private void processAIMessage(MapRecord<String, Object, Object> record) {
        String messageId = null;
        String topic = null;

        try {
            Map<Object, Object> data = record.getValue();
            messageId = (String) data.get("messageId");
            topic = (String) data.get("topic");
            String payloadStr = (String) data.get("payload");

            Map<String, Object> payload = objectMapper.readValue(payloadStr,
                    new TypeReference<Map<String, Object>>() {});

            logger.debug("开始处理AI消息: topic={}, messageId={}", topic, messageId);
            long startTime = System.currentTimeMillis();

            // 根据topic分发处理
            switch(topic) {
                case AIQueueTopics.QUESTION_GENERATION -> processQuestionGeneration(payload);
                case AIQueueTopics.FEEDBACK_GENERATION -> processFeedbackGeneration(payload);
                case AIQueueTopics.EMBEDDING_CALCULATION -> processEmbeddingCalculation(payload);
                case AIQueueTopics.FINAL_EVALUATION -> processFinalEvaluation(payload);
                default -> {
                    logger.warn("未知的AI Topic: {}", topic);
                    return;
                }
            }

            // 确认消息处理完成
            acknowledgeMessage(String.valueOf(record.getId()));

            long duration = System.currentTimeMillis() - startTime;
            logger.info("AI消息处理完成: topic={}, messageId={}, 耗时={}ms", topic, messageId, duration);

        } catch (Exception e) {
            logger.error("处理AI消息失败: topic={}, messageId={}", topic, messageId, e);
            handleProcessingError(record, e);
        }
    }

    /**
     * 处理开场题目生成 - 重构版，使用WebSocket响应队列
     */
    private void processQuestionGeneration(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        Long questionId = getLongValue(payload, "questionId");

        try {
            logger.debug("开始生成开场题目: sessionId={}, questionId={}", sessionId, questionId);

            // 发送处理状态到WebSocket响应队列
            webSocketResponseProducer.sendProcessingStatusMessage(sessionId, "generating", "AI正在准备开场题目...");

            // 获取题目信息
            Question question = getQuestionById(questionId);
            if (question == null) {
                logger.error("题目不存在: questionId={}", questionId);
                webSocketResponseProducer.sendAIResponseMessage(sessionId, "抱歉，无法获取题目信息。", "ERROR");
                webSocketResponseProducer.sendSessionStateMessage(sessionId, "ERROR", false);
                return;
            }

            // 使用现有的generateOpeningMessage方法
            String aiResponse = chatService.generateOpeningMessage(question);

            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                // 降级处理：使用默认开场消息
                aiResponse = "面试开始！请回答以下问题：\n\n" + question.getText();
                logger.warn("AI生成开场失败，使用默认消息: sessionId={}", sessionId);
            }

            // 使用现有的saveAIMessage方法保存消息
            MessageDTO aiMessage = chatService.saveAIMessage(sessionId, aiResponse);

            // 发送AI响应到WebSocket响应队列
            webSocketResponseProducer.sendAIResponseMessage(sessionId, aiResponse, "WAITING_FOR_USER_ANSWER");

            // 发送会话状态更新到WebSocket响应队列：启用输入框，用户可以开始回答
            webSocketResponseProducer.sendSessionStateMessage(sessionId, "WAITING_FOR_USER_ANSWER", true);

            logger.info("开场题目生成完成: sessionId={}, questionId={}, responseLength={}",
                    sessionId, questionId, aiResponse.length());

        } catch (Exception e) {
            logger.error("处理开场题目生成失败: sessionId={}", sessionId, e);

            // 错误处理：发送错误消息到WebSocket响应队列
            webSocketResponseProducer.sendAIResponseMessage(sessionId, "AI暂时无法生成题目，请稍后再试。", "ERROR");
            webSocketResponseProducer.sendSessionStateMessage(sessionId, "WAITING_FOR_USER_ANSWER", true);
        }
    }

    /**
     * 处理反馈生成 - 重构版，使用WebSocket响应队列和现有方法
     */
    private void processFeedbackGeneration(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        Long currentQuestionId = getLongValue(payload, "currentQuestionId");
        String userAnswer = (String) payload.get("userAnswer");
        Long nextQuestionId = getLongValue(payload, "nextQuestionId");

        try {
            String aiResponse;
            String newState;
            boolean chatEnabled;

            // 发送处理开始状态到WebSocket响应队列
            webSocketResponseProducer.sendProcessingStatusMessage(sessionId, "generating", "AI正在生成反馈...");

            if (nextQuestionId != null && nextQuestionId > 0) {
                // 有下一题，生成反馈+下一题
                Question nextQuestion = getQuestionById(nextQuestionId);
                if (nextQuestion == null) {
                    logger.error("下一题不存在: questionId={}", nextQuestionId);
                    webSocketResponseProducer.sendAIResponseMessage(sessionId, "抱歉，无法获取下一个问题。", "ERROR");
                    return;
                }

                // 使用现有的generateFeedbackWithNextQuestion方法
                aiResponse = chatService.generateFeedbackWithNextQuestion(userAnswer, currentQuestionId, nextQuestion);

                // 使用现有的SessionService方法更新状态
                sessionService.moveToNextQuestion(sessionId);

                newState = "WAITING_FOR_USER_ANSWER";
                chatEnabled = true;

                logger.info("反馈+下一题生成完成: sessionId={}, nextQuestionId={}", sessionId, nextQuestionId);

            } else {
                // 面试结束，生成最终评价
                aiResponse = chatService.generateFinalFeedback(sessionId, userAnswer, currentQuestionId);

                // 使用现有的SessionService方法结束会话
                sessionService.endSession(sessionId);

                newState = "INTERVIEW_COMPLETED";
                chatEnabled = false;

                logger.info("最终评价生成完成: sessionId={}", sessionId);
            }

            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                // 降级处理
                aiResponse = nextQuestionId != null ?
                        "感谢你的回答。请继续下一个问题。" :
                        "感谢你完成本次面试！";
                logger.warn("AI生成反馈失败，使用默认消息: sessionId={}", sessionId);
            }

            // 使用现有的saveAIMessage方法保存消息
            MessageDTO aiMessage = chatService.saveAIMessage(sessionId, aiResponse);

            // 发送AI响应到WebSocket响应队列
            webSocketResponseProducer.sendAIResponseMessage(sessionId, aiResponse, newState);

            // 发送会话状态更新到WebSocket响应队列
            webSocketResponseProducer.sendSessionStateMessage(sessionId, newState, chatEnabled);

        } catch (Exception e) {
            logger.error("处理反馈生成失败: sessionId={}", sessionId, e);

            // 错误处理：发送错误消息到WebSocket响应队列
            webSocketResponseProducer.sendAIResponseMessage(sessionId, "AI暂时无法生成反馈，请稍后再试。", "ERROR");
            webSocketResponseProducer.sendSessionStateMessage(sessionId, "WAITING_FOR_USER_ANSWER", true);
        }
    }

    /**
     * 处理embedding计算 - 保持不变，因为不涉及WebSocket推送
     */
    private void processEmbeddingCalculation(Map<String, Object> payload) {
        String type = (String) payload.get("type");

        try {
            switch (type) {
                case "single_embedding" -> processSingleEmbedding(payload);
                case "batch_embedding" -> processBatchEmbedding(payload);
                case "similarity_check" -> processSimilarityCheck(payload);
                default -> logger.warn("未知的embedding计算类型: {}", type);
            }

        } catch (Exception e) {
            logger.error("处理embedding计算失败: type={}", type, e);
        }
    }

    /**
     * 处理最终评价生成 - 重构版，使用WebSocket响应队列
     */
    private void processFinalEvaluation(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        String lastAnswer = (String) payload.get("lastAnswer");

        try {
            logger.debug("开始生成最终评价: sessionId={}", sessionId);

            // 发送处理状态到WebSocket响应队列
            webSocketResponseProducer.sendProcessingStatusMessage(sessionId, "generating", "AI正在生成最终评价...");

            // 使用现有的generateFinalEvaluation方法
            Long lastQuestionId = sessionService.getPreviousQuestionId(sessionId);
            String aiResponse = chatService.generateFinalFeedback(sessionId, lastAnswer, lastQuestionId);

            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                // 降级处理
                aiResponse = "感谢你完成本次面试！我们会尽快为你提供详细的评价反馈。";
                logger.warn("AI生成最终评价失败，使用默认消息: sessionId={}", sessionId);
            }

            // 使用现有的saveAIMessage和completeSession方法
            MessageDTO aiMessage = chatService.saveAIMessage(sessionId, aiResponse);
            sessionService.endSession(sessionId);

            // 发送AI响应到WebSocket响应队列
            webSocketResponseProducer.sendAIResponseMessage(sessionId, aiResponse, "COMPLETED");

            // 发送会话状态更新到WebSocket响应队列：面试结束，禁用输入框
            webSocketResponseProducer.sendSessionStateMessage(sessionId, "COMPLETED", false);

            logger.info("最终评价生成完成: sessionId={}, responseLength={}", sessionId, aiResponse.length());

        } catch (Exception e) {
            logger.error("处理最终评价生成失败: sessionId={}", sessionId, e);

            // 错误处理：发送错误消息到WebSocket响应队列
            webSocketResponseProducer.sendAIResponseMessage(sessionId, "AI暂时无法生成评价，请稍后再试。", "ERROR");
            webSocketResponseProducer.sendSessionStateMessage(sessionId, "COMPLETED", false);
        }
    }

    // 以下方法保持不变，因为它们不涉及WebSocket推送

    /**
     * 处理单个embedding计算
     */
    private void processSingleEmbedding(Map<String, Object> payload) {
        String text = (String) payload.get("text");
        String cacheKey = (String) payload.get("cacheKey");
        String context = (String) payload.get("context");

        try {
            // 这里应该调用embedding服务计算向量
            // 暂时跳过具体实现，因为不在本次重构范围内
            logger.debug("处理单个embedding计算: cacheKey={}", cacheKey);

        } catch (Exception e) {
            logger.error("单个embedding计算失败: cacheKey={}", cacheKey, e);
        }
    }

    /**
     * 处理批量embedding计算
     */
    private void processBatchEmbedding(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> textList = (List<Map<String, String>>) payload.get("textList");
        String batchId = (String) payload.get("batchId");

        try {
            // 这里应该调用embedding服务批量计算向量
            // 暂时跳过具体实现，因为不在本次重构范围内
            logger.debug("处理批量embedding计算: batchId={}, size={}", batchId,
                    textList != null ? textList.size() : 0);

        } catch (Exception e) {
            logger.error("批量embedding计算失败: batchId={}", batchId, e);
        }
    }

    /**
     * 处理相似度检查
     */
    private void processSimilarityCheck(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        Long questionId = getLongValue(payload, "questionId");
        String userAnswer = (String) payload.get("userAnswer");

        try {
            // 这里应该调用相似度检查服务
            // 暂时跳过具体实现，因为不在本次重构范围内
            logger.debug("处理相似度检查: sessionId={}, questionId={}", sessionId, questionId);

        } catch (Exception e) {
            logger.error("相似度检查失败: sessionId={}, questionId={}", sessionId, questionId, e);
        }
    }

    /**
     * 获取题目信息
     */
    private Question getQuestionById(Long questionId) {
        try {
            return questionMapper.findById(questionId).orElse(null);
        } catch (Exception e) {
            logger.error("获取题目失败: questionId={}", questionId, e);
            return null;
        }
    }

    /**
     * 获取Long类型值的工具方法
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
            String streamName = queueProperties.getStreams().getRequests();
            String groupName = queueProperties.getConsumer().getGroupName();

            redisTemplate.opsForStream().acknowledge(streamName, groupName, messageId);

        } catch (Exception e) {
            logger.error("确认AI消息失败: messageId={}", messageId, e);
        }
    }

    /**
     * 处理消息处理错误
     */
    private void handleProcessingError(MapRecord<String, Object, Object> record, Exception error) {
        try {
            Map<Object, Object> data = record.getValue();
            String messageId = (String) data.get("messageId");
            Integer retryCount = (Integer) data.get("retryCount");
            int currentRetryCount = retryCount != null ? retryCount : 0;

            if (currentRetryCount < queueProperties.getProcessors().getMaxRetries()) {
                logger.warn("AI消息处理失败，准备重试: messageId={}, 当前重试次数={}",
                        messageId, currentRetryCount);
                // 这里可以实现重试机制，但为了简单起见，暂时只记录日志
            } else {
                logger.error("AI消息处理达到最大重试次数，放弃: messageId={}", messageId);
                // 确认消息以避免重复处理
                acknowledgeMessage(String.valueOf(record.getId()));
            }

        } catch (Exception e) {
            logger.error("处理AI消息错误时出错", e);
        }
    }
}