// AIQueueConsumer.java - 修正版，使用现有ChatService方法
package com.xinyu.InterviewCoach_v2.queue.consumer;

import com.xinyu.InterviewCoach_v2.mapper.QuestionMapper;
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
    private QuestionMapper questionMapper;

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
     * 处理开场题目生成 - 改进版，完善状态推送
     */
    private void processQuestionGeneration(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        Long questionId = getLongValue(payload, "questionId");

        try {
            logger.debug("开始生成开场题目: sessionId={}, questionId={}", sessionId, questionId);

            // 推送开始处理状态
            webSocketService.pushAIProcessingStatus(sessionId, "generating", "AI正在准备开场题目...");

            // 获取题目信息
            Question question = getQuestionById(questionId);
            if (question == null) {
                logger.error("题目不存在: questionId={}", questionId);
                webSocketService.pushAIResponse(sessionId, "抱歉，无法获取题目信息。", "ERROR");
                webSocketService.pushSessionStateUpdate(sessionId, "ERROR", false);
                return;
            }

            // 使用现有的generateOpeningMessage方法（通过反射调用私有方法）
            String aiResponse = chatService.generateOpeningMessage(question);

            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                // 降级处理：使用默认开场消息
                aiResponse = "面试开始！请回答以下问题：\n\n" + question.getText();
                logger.warn("AI生成开场失败，使用默认消息: sessionId={}", sessionId);
            }

            // 使用现有的saveAIMessage方法保存消息
            MessageDTO aiMessage = chatService.saveAIMessage(sessionId, aiResponse);

            // 推送开场消息给前端
            webSocketService.pushAIResponse(sessionId, aiResponse, "WAITING_FOR_USER_ANSWER");

            // 推送会话状态更新：启用输入框，用户可以开始回答
            webSocketService.pushSessionStateUpdate(sessionId, "WAITING_FOR_USER_ANSWER", true);

            logger.info("开场题目生成完成: sessionId={}, questionId={}, responseLength={}",
                    sessionId, questionId, aiResponse.length());

        } catch (Exception e) {
            logger.error("处理开场题目生成失败: sessionId={}", sessionId, e);

            // 错误处理：推送错误消息和恢复状态
            webSocketService.pushAIResponse(sessionId, "AI暂时无法生成题目，请稍后再试。", "ERROR");
            webSocketService.pushSessionStateUpdate(sessionId, "WAITING_FOR_USER_ANSWER", true);
        }
    }

    /**
     * 处理反馈生成 - 修正版，使用正确的ChatService方法
     */
    private void processFeedbackGeneration(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        Long currentQuestionId = getLongValue(payload, "currentQuestionId"); // 这是上一题ID
        String userAnswer = (String) payload.get("userAnswer");
        Long nextQuestionId = getLongValue(payload, "nextQuestionId");

        try {
            String aiResponse;
            String newState;
            boolean chatEnabled;

            // 推送处理开始状态
            webSocketService.pushAIProcessingStatus(sessionId, "generating", "AI正在生成反馈...");

            if (nextQuestionId != null && nextQuestionId > 0) {
                // 有下一题，生成反馈+下一题
                Question nextQuestion = getQuestionById(nextQuestionId);
                if (nextQuestion == null) {
                    logger.error("下一题不存在: questionId={}", nextQuestionId);
                    webSocketService.pushAIResponse(sessionId, "抱歉，无法获取下一个问题。", "ERROR");
                    return;
                }

                // 使用现有的generateFeedbackWithNextQuestion方法
                // 参数：userAnswer, previousQuestionId, nextQuestion
                aiResponse = chatService.generateFeedbackWithNextQuestion(userAnswer, currentQuestionId, nextQuestion);

                // 使用现有的SessionService方法更新状态
                sessionService.moveToNextQuestion(sessionId);
                sessionService.incrementCompletedQuestionCount(sessionId);

                newState = "WAITING_FOR_USER_ANSWER";
                chatEnabled = true;

            } else {
                // 面试结束，生成最终评价
                aiResponse = chatService.generateFinalFeedback(sessionId, userAnswer, currentQuestionId);

                // 使用现有的SessionService方法结束会话
                sessionService.endSession(sessionId);

                newState = "INTERVIEW_COMPLETED";
                chatEnabled = false;
            }

            // 使用现有的saveAIMessage方法保存消息
            MessageDTO aiMessage = chatService.saveAIMessage(sessionId, aiResponse);

            // 推送AI回复给前端（包含状态更新）
            webSocketService.pushAIResponse(sessionId, aiResponse, newState);

            // 推送会话状态更新（恢复输入框状态）
            webSocketService.pushSessionStateUpdate(sessionId, newState, chatEnabled);

            logger.info("反馈生成完成: sessionId={}, currentQuestionId={}, hasNext={}",
                    sessionId, currentQuestionId, nextQuestionId != null);

        } catch (Exception e) {
            logger.error("处理反馈生成失败: sessionId={}", sessionId, e);

            // 错误时恢复输入框，让用户可以重试
            webSocketService.pushAIResponse(sessionId, "AI暂时无法回应，请稍后再试。", "ERROR");
            webSocketService.pushSessionStateUpdate(sessionId, "WAITING_FOR_USER_ANSWER", true);
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
     * 处理最终评价生成 - 改进版，完善状态推送和错误处理
     */
    private void processFinalEvaluation(Map<String, Object> payload) {
        Long sessionId = getLongValue(payload, "sessionId");
        String lastAnswer = (String) payload.get("lastAnswer");

        try {
            logger.debug("开始生成最终评价: sessionId={}", sessionId);

            // 推送开始处理状态
            webSocketService.pushAIProcessingStatus(sessionId, "evaluating", "AI正在生成最终评价...");

            // 获取最后一题ID作为参考
            Long lastQuestionId = sessionService.getPreviousQuestionId(sessionId);

            // 使用现有的generateFinalFeedback方法
            String evaluation = chatService.generateFinalFeedback(sessionId, lastAnswer, lastQuestionId);

            if (evaluation == null || evaluation.trim().isEmpty()) {
                // 降级处理：使用默认评价
                evaluation = "感谢您参加本次面试！您在面试中表现良好，展现了扎实的技术基础。继续保持学习和实践，祝您求职顺利！";
                logger.warn("AI生成最终评价失败，使用默认评价: sessionId={}", sessionId);
            }

            // 使用现有的saveAIMessage方法保存消息
            MessageDTO aiMessage = chatService.saveAIMessage(sessionId, evaluation);

            // 使用现有的SessionService.endSession方法结束会话
            sessionService.endSession(sessionId);

            // 推送最终评价给前端
            webSocketService.pushAIResponse(sessionId, evaluation, "INTERVIEW_COMPLETED");

            // 推送会话最终状态：面试完成，禁用输入
            webSocketService.pushSessionStateUpdate(sessionId, "INTERVIEW_COMPLETED", false);

            logger.info("最终评价生成完成: sessionId={}, evaluationLength={}",
                    sessionId, evaluation.length());

        } catch (Exception e) {
            logger.error("处理最终评价失败: sessionId={}", sessionId, e);

            // 错误时推送友好的结束消息
            String fallbackMessage = "感谢您参加本次面试！虽然AI评价暂时无法生成，但您的表现值得肯定。祝您求职顺利！";

            webSocketService.pushAIResponse(sessionId, fallbackMessage, "INTERVIEW_COMPLETED");
            webSocketService.pushSessionStateUpdate(sessionId, "INTERVIEW_COMPLETED", false);

            // 确保会话正常结束
            try {
                sessionService.endSession(sessionId);
            } catch (Exception endSessionError) {
                logger.error("结束会话失败: sessionId={}", sessionId, endSessionError);
            }
        }
    }


    /**
     * 获取题目信息 - 使用反射调用questionMapper
     */
    private Question getQuestionById(Long questionId) {
        try {
            return questionMapper.findById(questionId).orElse(null);
        } catch (Exception e) {
            logger.error("获取题目失败: questionId={}", questionId, e);
            return null;
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