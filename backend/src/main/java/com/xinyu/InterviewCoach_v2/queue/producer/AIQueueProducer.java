package com.xinyu.InterviewCoach_v2.queue.producer;

import com.xinyu.InterviewCoach_v2.dto.queue.AIQueueMessage;
import com.xinyu.InterviewCoach_v2.queue.constants.AIQueueTopics;
import com.xinyu.InterviewCoach_v2.config.properties.AIQueueProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AIQueueProducer {

    private static final Logger logger = LoggerFactory.getLogger(AIQueueProducer.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AIQueueProperties queueProperties;

    /**
     * 发送开场题目生成请求
     */
    public void sendOpeningQuestionRequest(Long sessionId, Long questionId) {
        if (!isTopicEnabled(AIQueueTopics.QUESTION_GENERATION)) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "sessionId", sessionId,
                "questionId", questionId,
                "type", "opening_question",
                "requestTime", System.currentTimeMillis()
        );

        sendMessage(AIQueueTopics.QUESTION_GENERATION, payload, AIQueueTopics.PRIORITY_HIGH);
        logger.info("发送开场题目生成请求: sessionId={}, questionId={}", sessionId, questionId);
    }

    /**
     * 发送反馈+下一题生成请求
     */
    public void sendFeedbackWithNextQuestionRequest(Long sessionId, Long currentQuestionId,
                                                    String userAnswer, Long nextQuestionId) {
        if (!isTopicEnabled(AIQueueTopics.FEEDBACK_GENERATION)) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "sessionId", sessionId,
                "currentQuestionId", currentQuestionId,
                "userAnswer", userAnswer,
                "nextQuestionId", nextQuestionId,
                "type", "feedback_with_next_question",
                "requestTime", System.currentTimeMillis()
        );

        sendMessage(AIQueueTopics.FEEDBACK_GENERATION, payload, AIQueueTopics.PRIORITY_HIGH);
        logger.info("发送反馈生成请求: sessionId={}, currentQuestionId={}", sessionId, currentQuestionId);
    }

    /**
     * 发送最终评价生成请求
     */
    public void sendFinalEvaluationRequest(Long sessionId, String lastAnswer) {
        if (!isTopicEnabled(AIQueueTopics.FINAL_EVALUATION)) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "sessionId", sessionId,
                "lastAnswer", lastAnswer != null ? lastAnswer : "",
                "type", "final_evaluation",
                "requestTime", System.currentTimeMillis()
        );

        sendMessage(AIQueueTopics.FINAL_EVALUATION, payload, AIQueueTopics.PRIORITY_LOW);
        logger.info("发送最终评价请求: sessionId={}", sessionId);
    }

    /**
     * 发送单个文本embedding计算请求
     */
    public void sendSingleEmbeddingRequest(String text, String cacheKey, String context) {
        if (!isTopicEnabled(AIQueueTopics.EMBEDDING_CALCULATION)) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "text", text,
                "cacheKey", cacheKey,
                "context", context != null ? context : "",
                "type", "single_embedding",
                "requestTime", System.currentTimeMillis()
        );

        sendMessage(AIQueueTopics.EMBEDDING_CALCULATION, payload, AIQueueTopics.PRIORITY_MEDIUM);
        logger.debug("发送embedding计算请求: cacheKey={}", cacheKey);
    }

    /**
     * 发送批量embedding计算请求
     */
    public void sendBatchEmbeddingRequest(List<Map<String, String>> textList, String batchId) {
        if (!isTopicEnabled(AIQueueTopics.EMBEDDING_CALCULATION)) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "textList", textList,
                "batchId", batchId,
                "batchSize", textList.size(),
                "type", "batch_embedding",
                "requestTime", System.currentTimeMillis()
        );

        sendMessage(AIQueueTopics.EMBEDDING_CALCULATION, payload, AIQueueTopics.PRIORITY_MEDIUM);
        logger.info("发送批量embedding请求: batchId={}, size={}", batchId, textList.size());
    }

    /**
     * 发送用户答案相似度检查请求
     */
    public void sendAnswerSimilarityRequest(Long sessionId, Long questionId, String userAnswer) {
        if (!isTopicEnabled(AIQueueTopics.EMBEDDING_CALCULATION)) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "sessionId", sessionId,
                "questionId", questionId,
                "userAnswer", userAnswer,
                "type", "similarity_check",
                "requestTime", System.currentTimeMillis()
        );

        sendMessage(AIQueueTopics.EMBEDDING_CALCULATION, payload, AIQueueTopics.PRIORITY_HIGH);
        logger.debug("发送答案相似度检查请求: sessionId={}, questionId={}", sessionId, questionId);
    }

    private boolean isTopicEnabled(String topic) {
        if (!queueProperties.isEnabled()) {
            logger.debug("AI队列整体未启用");
            return false;
        }

        return switch(topic) {
            case AIQueueTopics.QUESTION_GENERATION ->
                    queueProperties.getTopics().getQuestionGeneration().isEnabled();
            case AIQueueTopics.FEEDBACK_GENERATION ->
                    queueProperties.getTopics().getFeedbackGeneration().isEnabled();
            case AIQueueTopics.EMBEDDING_CALCULATION ->
                    queueProperties.getTopics().getEmbeddingCalculation().isEnabled();
            case AIQueueTopics.FINAL_EVALUATION ->
                    queueProperties.getTopics().getFinalEvaluation().isEnabled();
            default -> false;
        };
    }

    private void sendMessage(String topic, Map<String, Object> payload, String priority) {
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

            redisTemplate.opsForStream().add(queueProperties.getStreams().getRequests(), streamRecord);

        } catch (Exception e) {
            logger.error("发送AI消息到队列失败: topic={}", topic, e);
            throw new RuntimeException("AI队列操作失败", e);
        }
    }
}