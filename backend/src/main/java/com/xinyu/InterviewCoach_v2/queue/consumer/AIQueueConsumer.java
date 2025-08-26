// AIQueueConsumer.java - 修正版，使用现有ChatService方法
package com.xinyu.InterviewCoach_v2.queue.consumer;

import com.xinyu.InterviewCoach_v2.queue.constants.AIQueueTopics;
import com.xinyu.InterviewCoach_v2.service.ChatService;
import com.xinyu.InterviewCoach_v2.service.WebSocketService;
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
import java.lang.reflect.Method;
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
    private ChatService chatService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private WebSocketService webSocketService;

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
     * 处理开场题目生成 - 使用现有方法
     */
    private void processQuestionGeneration(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        Long questionId = getLongValue(payload, "questionId");

        try {
            // 获取题目信息
            Question question = getQuestionById(questionId);
            if (question == null) {
                logger.error("题目不存在: questionId={}", questionId);
                webSocketService.pushAIResponse(sessionId, "抱歉，无法获取题目信息。", "ERROR");
                return;
            }

            // 使用现有的generateOpeningMessage方法（通过反射调用私有方法）
            String aiResponse = callPrivateMethod(chatService, "generateOpeningMessage",
                    new Class[]{Question.class}, new Object[]{question});

            if (aiResponse == null) {
                aiResponse = "面试开始！请回答以下问题：\n\n" + question.getText();
            }

            // 使用现有的saveAIMessage方法（通过反射调用私有方法）
            MessageDTO aiMessage = callPrivateMethod(chatService, "saveAIMessage",
                    new Class[]{Long.class, String.class}, new Object[]{sessionId, aiResponse});

            // 通过WebSocket推送给前端
            webSocketService.pushAIResponse(sessionId, aiResponse, "ASKING_QUESTION");

            logger.info("开场题目生成完成: sessionId={}, questionId={}", sessionId, questionId);

        } catch (Exception e) {
            logger.error("处理开场题目生成失败: sessionId={}", sessionId, e);
            webSocketService.pushAIResponse(sessionId, "AI暂时无法生成题目，请稍后再试。", "ERROR");
        }
    }

    /**
     * 处理反馈生成 - 使用现有方法
     */
    private void processFeedbackGeneration(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        Long currentQuestionId = getLongValue(payload, "currentQuestionId");
        String userAnswer = (String) payload.get("userAnswer");
        Long nextQuestionId = getLongValue(payload, "nextQuestionId");

        try {
            String aiResponse;
            String newState;

            if (nextQuestionId != null && nextQuestionId > 0) {
                // 有下一题，生成反馈+下一题
                Question nextQuestion = getQuestionById(nextQuestionId);
                if (nextQuestion == null) {
                    logger.error("下一题不存在: questionId={}", nextQuestionId);
                    webSocketService.pushAIResponse(sessionId, "抱歉，无法获取下一个问题。", "ERROR");
                    return;
                }

                // 使用现有的generateFeedbackWithNextQuestion方法
                aiResponse = callPrivateMethod(chatService, "generateFeedbackWithNextQuestion",
                        new Class[]{String.class, Long.class, Question.class},
                        new Object[]{userAnswer, currentQuestionId, nextQuestion});

                // 使用现有的SessionService方法更新状态
                sessionService.moveToNextQuestion(sessionId);
                sessionService.incrementCompletedQuestionCount(sessionId);

                newState = "WAITING_FOR_USER_ANSWER";

            } else {
                // 面试结束，生成最终评价
                aiResponse = callPrivateMethod(chatService, "generateFinalFeedback",
                        new Class[]{Long.class, String.class, Long.class},
                        new Object[]{sessionId, userAnswer, currentQuestionId});

                // 使用现有的SessionService方法结束会话
                sessionService.endSession(sessionId);

                newState = "INTERVIEW_COMPLETED";
            }

            // 使用现有的saveAIMessage方法保存消息
            MessageDTO aiMessage = callPrivateMethod(chatService, "saveAIMessage",
                    new Class[]{Long.class, String.class}, new Object[]{sessionId, aiResponse});

            // 推送给前端
            webSocketService.pushAIResponse(sessionId, aiResponse, newState);

            logger.info("反馈生成完成: sessionId={}, currentQuestionId={}", sessionId, currentQuestionId);

        } catch (Exception e) {
            logger.error("处理反馈生成失败: sessionId={}", sessionId, e);
            webSocketService.pushAIResponse(sessionId, "AI暂时无法回应，请稍后再试。", "ERROR");
        }
    }

    /**
     * 处理embedding计算 - 使用现有缓存管理器方法
     */
    private void processEmbeddingCalculation(Map<String, Object> payload) {
        String type = (String) payload.get("type");

        try {
            switch(type) {
                case "single_embedding" -> {
                    String text = (String) payload.get("text");
                    String cacheKey = (String) payload.get("cacheKey");

                    // 使用AIResponseCacheManager的新增方法
                    aiCacheManager.calculateAndCacheEmbedding(text, cacheKey);
                    logger.debug("单个embedding计算完成: cacheKey={}", cacheKey);
                }

                case "batch_embedding" -> {
                    List<Map<String, String>> textList = (List<Map<String, String>>) payload.get("textList");
                    String batchId = (String) payload.get("batchId");

                    Map<String, String> textCacheMap = new HashMap<>();
                    for (Map<String, String> item : textList) {
                        textCacheMap.put(item.get("text"), item.get("cacheKey"));
                    }

                    // 使用AIResponseCacheManager的新增方法
                    aiCacheManager.batchCalculateEmbeddings(textCacheMap);
                    logger.info("批量embedding计算完成: batchId={}, size={}", batchId, textList.size());
                }

                default -> logger.warn("未知的embedding计算类型: {}", type);
            }
        } catch (Exception e) {
            logger.error("处理embedding计算失败: type={}", type, e);
        }
    }

    /**
     * 处理最终评价生成 - 使用现有方法
     */
    private void processFinalEvaluation(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        String lastAnswer = (String) payload.get("lastAnswer");

        try {
            // 获取当前题目ID作为最后一题
            Long lastQuestionId = sessionService.getPreviousQuestionId(sessionId);

            // 使用现有的generateFinalFeedback方法
            String evaluation = callPrivateMethod(chatService, "generateFinalFeedback",
                    new Class[]{Long.class, String.class, Long.class},
                    new Object[]{sessionId, lastAnswer, lastQuestionId});

            // 使用现有的saveAIMessage方法
            MessageDTO aiMessage = callPrivateMethod(chatService, "saveAIMessage",
                    new Class[]{Long.class, String.class}, new Object[]{sessionId, evaluation});

            // 使用现有的SessionService.endSession方法
            sessionService.endSession(sessionId);

            // 推送给前端
            webSocketService.pushAIResponse(sessionId, evaluation, "EVALUATION_COMPLETED");

            logger.info("最终评价生成完成: sessionId={}", sessionId);

        } catch (Exception e) {
            logger.error("处理最终评价失败: sessionId={}", sessionId, e);
            webSocketService.pushAIResponse(sessionId, "感谢您参加本次面试！", "EVALUATION_COMPLETED");
        }
    }

    // ===== 辅助方法 =====

    /**
     * 获取题目信息 - 使用反射调用questionMapper
     */
    private Question getQuestionById(Long questionId) {
        try {
            // 通过反射访问ChatService中的questionMapper
            java.lang.reflect.Field field = ChatService.class.getDeclaredField("questionMapper");
            field.setAccessible(true);
            Object questionMapper = field.get(chatService);

            Method findByIdMethod = questionMapper.getClass().getMethod("findById", Long.class);
            Optional<Question> result = (Optional<Question>) findByIdMethod.invoke(questionMapper, questionId);

            return result.orElse(null);

        } catch (Exception e) {
            logger.error("获取题目失败: questionId={}", questionId, e);
            return null;
        }
    }

    /**
     * 通过反射调用ChatService的私有方法
     */
    @SuppressWarnings("unchecked")
    private <T> T callPrivateMethod(Object target, String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return (T) method.invoke(target, args);
        } catch (Exception e) {
            logger.error("反射调用方法失败: method={}", methodName, e);
            throw new RuntimeException("调用ChatService方法失败: " + methodName, e);
        }
    }

    private Long getLongValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private void acknowledgeMessage(String recordId) {
        try {
            redisTemplate.opsForStream().acknowledge(
                    queueProperties.getConsumer().getGroupName(),
                    queueProperties.getStreams().getRequests(),
                    recordId);
        } catch (Exception e) {
            logger.error("确认消息失败: recordId={}", recordId, e);
        }
    }

    private void handleProcessingError(MapRecord<String, Object, Object> record, Throwable error) {
        String messageId = (String) record.getValue().get("messageId");
        logger.error("AI消息处理失败，将确认消息: messageId={}", messageId, error);
        acknowledgeMessage(String.valueOf(record.getId()));
    }
}