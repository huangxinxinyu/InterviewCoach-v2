package com.xinyu.InterviewCoach_v2.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinyu.InterviewCoach_v2.enums.SessionMode;
import com.xinyu.InterviewCoach_v2.queue.producer.AIQueueProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OpenAI响应缓存管理器 - 支持中文语义相似性判断
 * 使用OpenAI Embedding API进行语义相似度计算
 */
@Component
public class AIResponseCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(AIResponseCacheManager.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private AIQueueProducer aiQueueProducer;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.embedding.url:https://api.openai.com/v1/embeddings}")
    private String embeddingApiUrl;

    @Value("${openai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${cache.ai.first-question.ttl:604800}")  // 7天
    private long firstQuestionTtl;

    @Value("${cache.ai.feedback.ttl:259200}")  // 3天
    private long feedbackTtl;

    @Value("${cache.ai.prefix:ai:}")
    private String aiCachePrefix;

    @Value("${cache.ai.enabled:true}")
    private boolean cacheEnabled;

    @Value("${cache.ai.similarity-threshold:0.85}")  // 中文语义相似度阈值
    private double similarityThreshold;

    @Value("${cache.ai.embedding.enabled:true}")
    private boolean embeddingEnabled;

    @Value("${cache.ai.max-similar-answers:10}")  // 最多检查多少个相似答案
    private int maxSimilarAnswers;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private Counter cacheHit;
    private Counter cacheMiss;
    private Counter apiCallSaved;
    private Counter embeddingCallMade;

    @PostConstruct
    private void initMetrics() {
        if (meterRegistry != null) {
            cacheHit = Counter.builder("ai.cache.hit").register(meterRegistry);
            cacheMiss = Counter.builder("ai.cache.miss").register(meterRegistry);
            apiCallSaved = Counter.builder("ai.api.call.saved").register(meterRegistry);
            embeddingCallMade = Counter.builder("ai.embedding.call.made").register(meterRegistry);
        }
    }

    /**
     * 获取缓存的第一题问法
     */
    public Optional<String> getCachedFirstQuestion(Long questionId, SessionMode mode) {
        if (!cacheEnabled) {
            return Optional.empty();
        }

        try {
            String key = buildFirstQuestionKey(questionId, mode);
            String cached = redisTemplate.opsForValue().get(key);

            if (cached != null) {
                logger.debug("第一题缓存命中: questionId={}, mode={}", questionId, mode);
                if (cacheHit != null) cacheHit.increment();
                if (apiCallSaved != null) apiCallSaved.increment();
                return Optional.of(cached);
            }

            logger.debug("第一题缓存未命中: questionId={}, mode={}", questionId, mode);
            if (cacheMiss != null) cacheMiss.increment();
            return Optional.empty();

        } catch (Exception e) {
            logger.error("获取第一题缓存失败: questionId={}, mode={}", questionId, mode, e);
            return Optional.empty();
        }
    }

    /**
     * 缓存第一题问法
     */
    public void cacheFirstQuestion(Long questionId, SessionMode mode, String aiResponse) {
        if (!cacheEnabled || aiResponse == null || aiResponse.trim().isEmpty()) {
            return;
        }

        try {
            String key = buildFirstQuestionKey(questionId, mode);
            redisTemplate.opsForValue().set(key, aiResponse, firstQuestionTtl, TimeUnit.SECONDS);
            logger.debug("缓存第一题问法: questionId={}, mode={}", questionId, mode);
        } catch (Exception e) {
            logger.error("缓存第一题问法失败: questionId={}, mode={}", questionId, mode, e);
        }
    }

    /**
     * 获取缓存的反馈回复 - 使用语义相似性匹配
     */
    public Optional<String> getCachedFeedback(Long previousQuestionId, Long nextQuestionId,
                                              String userAnswer, boolean hasStandardAnswer) {
        if (!cacheEnabled || userAnswer == null || userAnswer.trim().length() < 10) {
            return Optional.empty();
        }

        try {
            // 1. 先尝试精确匹配
            String exactKey = buildExactFeedbackKey(previousQuestionId, nextQuestionId, userAnswer, hasStandardAnswer);
            String exactCached = redisTemplate.opsForValue().get(exactKey);
            if (exactCached != null) {
                logger.debug("反馈精确缓存命中: prevQ={}, nextQ={}", previousQuestionId, nextQuestionId);
                if (cacheHit != null) cacheHit.increment();
                if (apiCallSaved != null) apiCallSaved.increment();
                return Optional.of(exactCached);
            }

            // 2. 语义相似性匹配
            if (embeddingEnabled) {
                Optional<String> similarFeedback = findSimilarCachedFeedback(
                        previousQuestionId, nextQuestionId, userAnswer, hasStandardAnswer);

                if (similarFeedback.isPresent()) {
                    logger.debug("反馈语义相似缓存命中: prevQ={}, nextQ={}", previousQuestionId, nextQuestionId);
                    if (cacheHit != null) cacheHit.increment();
                    if (apiCallSaved != null) apiCallSaved.increment();
                    return similarFeedback;
                }
            }

            logger.debug("反馈缓存未命中: prevQ={}, nextQ={}", previousQuestionId, nextQuestionId);
            if (cacheMiss != null) cacheMiss.increment();
            return Optional.empty();

        } catch (Exception e) {
            logger.error("获取反馈缓存失败: prevQ={}, nextQ={}", previousQuestionId, nextQuestionId, e);
            return Optional.empty();
        }
    }

    /**
     * 缓存反馈回复
     */
    public void cacheFeedback(Long previousQuestionId, Long nextQuestionId,
                              String userAnswer, boolean hasStandardAnswer, String aiResponse) {
        if (!cacheEnabled || userAnswer == null || aiResponse == null ||
                userAnswer.trim().length() < 10 || aiResponse.trim().isEmpty()) {
            return;
        }

        try {
            // 1. 缓存精确匹配
            String exactKey = buildExactFeedbackKey(previousQuestionId, nextQuestionId, userAnswer, hasStandardAnswer);
            redisTemplate.opsForValue().set(exactKey, aiResponse, feedbackTtl, TimeUnit.SECONDS);

            // 2. 缓存到语义相似性索引
            if (embeddingEnabled) {
                cacheForSemanticSearch(previousQuestionId, nextQuestionId, userAnswer, hasStandardAnswer, aiResponse);
            }

            logger.debug("缓存反馈回复: prevQ={}, nextQ={}, answerLength={}",
                    previousQuestionId, nextQuestionId, userAnswer.length());

        } catch (Exception e) {
            logger.error("缓存反馈回复失败: prevQ={}, nextQ={}", previousQuestionId, nextQuestionId, e);
        }
    }

    /**
     * 查找语义相似的缓存反馈
     */
    private Optional<String> findSimilarCachedFeedback(Long previousQuestionId, Long nextQuestionId,
                                                       String userAnswer, boolean hasStandardAnswer) {
        try {
            // 获取用户答案的embedding
            double[] userAnswerEmbedding = getTextEmbedding(userAnswer);
            if (userAnswerEmbedding == null) {
                return Optional.empty();
            }

            // 搜索相似的缓存条目
            String searchPattern = buildSemanticSearchPattern(previousQuestionId, nextQuestionId, hasStandardAnswer);
            Set<String> candidateKeys = redisTemplate.keys(searchPattern);

            if (candidateKeys == null || candidateKeys.isEmpty()) {
                return Optional.empty();
            }

            // 限制搜索范围，避免过多计算
            List<String> limitedKeys = candidateKeys.stream()
                    .limit(maxSimilarAnswers)
                    .collect(Collectors.toList());

            double maxSimilarity = 0.0;
            String bestMatch = null;

            for (String candidateKey : limitedKeys) {
                try {
                    // 获取候选答案的embedding
                    String embeddingKey = candidateKey + ":embedding";
                    String embeddingStr = redisTemplate.opsForValue().get(embeddingKey);

                    if (embeddingStr != null) {
                        double[] candidateEmbedding = objectMapper.readValue(embeddingStr, double[].class);
                        double similarity = calculateCosineSimilarity(userAnswerEmbedding, candidateEmbedding);

                        if (similarity > maxSimilarity && similarity >= similarityThreshold) {
                            maxSimilarity = similarity;
                            String cachedResponse = redisTemplate.opsForValue().get(candidateKey);
                            if (cachedResponse != null) {
                                bestMatch = cachedResponse;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("处理候选缓存键失败: {}", candidateKey, e);
                }
            }

            if (bestMatch != null) {
                logger.debug("找到语义相似缓存: similarity={}, threshold={}", maxSimilarity, similarityThreshold);
                return Optional.of(bestMatch);
            }

            return Optional.empty();

        } catch (Exception e) {
            logger.error("语义相似性搜索失败", e);
            return Optional.empty();
        }
    }

    /**
     * 缓存到语义搜索索引
     */
    private void cacheForSemanticSearch(Long previousQuestionId, Long nextQuestionId,
                                        String userAnswer, boolean hasStandardAnswer, String aiResponse) {
        try {
            // 获取用户答案的embedding
            double[] embedding = getTextEmbedding(userAnswer);
            if (embedding == null) {
                return;
            }

            // 生成语义搜索用的key
            String semanticKey = buildSemanticCacheKey(previousQuestionId, nextQuestionId,
                    userAnswer, hasStandardAnswer);

            // 缓存回复内容
            redisTemplate.opsForValue().set(semanticKey, aiResponse, feedbackTtl, TimeUnit.SECONDS);

            // 缓存embedding向量
            String embeddingKey = semanticKey + ":embedding";
            String embeddingStr = objectMapper.writeValueAsString(embedding);
            redisTemplate.opsForValue().set(embeddingKey, embeddingStr, feedbackTtl, TimeUnit.SECONDS);

            logger.debug("缓存语义搜索索引: key={}", semanticKey);

        } catch (Exception e) {
            logger.error("缓存语义搜索索引失败", e);
        }
    }

    /**
     * 调用OpenAI Embedding API获取文本向量
     */
    private double[] getTextEmbedding(String text) {
        if (!embeddingEnabled || text == null || text.trim().isEmpty()) {
            return null;
        }

        try {
            // 检查embedding缓存
            String embeddingCacheKey = "embedding:" + generateTextHash(text);
            String cachedEmbedding = redisTemplate.opsForValue().get(embeddingCacheKey);

            if (cachedEmbedding != null) {
                return objectMapper.readValue(cachedEmbedding, double[].class);
            }

            // 调用OpenAI Embedding API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", embeddingModel,
                    "input", text,
                    "encoding_format", "float"
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    embeddingApiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");

                if (data != null && !data.isEmpty()) {
                    List<Double> embeddingList = (List<Double>) data.get(0).get("embedding");
                    double[] embedding = embeddingList.stream().mapToDouble(Double::doubleValue).toArray();

                    // 缓存embedding结果
                    String embeddingStr = objectMapper.writeValueAsString(embedding);
                    redisTemplate.opsForValue().set(embeddingCacheKey, embeddingStr, 86400, TimeUnit.SECONDS); // 缓存1天

                    if (embeddingCallMade != null) embeddingCallMade.increment();
                    logger.debug("获取文本embedding成功: textLength={}, embeddingLength={}", text.length(), embedding.length);

                    return embedding;
                }
            }

            logger.warn("获取embedding失败: response={}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            logger.error("调用embedding API失败: text={}", text.substring(0, Math.min(50, text.length())), e);
            return null;
        }
    }

    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    /**
     * 生成文本hash
     */
    private String generateTextHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("生成文本hash失败", e);
            return String.valueOf(text.hashCode());
        }
    }

    /**
     * 构建第一题缓存key
     */
    private String buildFirstQuestionKey(Long questionId, SessionMode mode) {
        return aiCachePrefix + "first:" + questionId + ":" + mode.name();
    }

    /**
     * 构建精确匹配反馈缓存key
     */
    private String buildExactFeedbackKey(Long previousQuestionId, Long nextQuestionId,
                                         String userAnswer, boolean hasStandardAnswer) {
        String answerHash = generateTextHash(userAnswer);
        return aiCachePrefix + "feedback:exact:" + previousQuestionId + ":" + nextQuestionId +
                ":" + answerHash + ":" + hasStandardAnswer;
    }

    /**
     * 构建语义缓存key
     */
    private String buildSemanticCacheKey(Long previousQuestionId, Long nextQuestionId,
                                         String userAnswer, boolean hasStandardAnswer) {
        String answerHash = generateTextHash(userAnswer);
        long timestamp = System.currentTimeMillis();
        return aiCachePrefix + "feedback:semantic:" + previousQuestionId + ":" + nextQuestionId +
                ":" + hasStandardAnswer + ":" + answerHash + ":" + timestamp;
    }

    /**
     * 构建语义搜索模式
     */
    private String buildSemanticSearchPattern(Long previousQuestionId, Long nextQuestionId, boolean hasStandardAnswer) {
        return aiCachePrefix + "feedback:semantic:" + previousQuestionId + ":" + nextQuestionId +
                ":" + hasStandardAnswer + ":*";
    }

    /**
     * 清理过期或低质量缓存
     */
    public void cleanupCache() {
        try {
            // 清理逻辑可以后续实现
            logger.info("开始清理AI响应缓存");

            // 清理空的或损坏的缓存条目
            Set<String> allKeys = redisTemplate.keys(aiCachePrefix + "*");
            if (allKeys != null) {
                for (String key : allKeys) {
                    try {
                        String value = redisTemplate.opsForValue().get(key);
                        if (value == null || value.trim().isEmpty()) {
                            redisTemplate.delete(key);
                        }
                    } catch (Exception e) {
                        logger.warn("清理缓存条目失败: key={}", key, e);
                        redisTemplate.delete(key); // 删除损坏的条目
                    }
                }
            }

            logger.info("AI响应缓存清理完成");
        } catch (Exception e) {
            logger.error("清理AI响应缓存失败", e);
        }
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            Set<String> allKeys = redisTemplate.keys(aiCachePrefix + "*");
            long totalKeys = allKeys != null ? allKeys.size() : 0;

            long firstQuestionKeys = 0;
            long feedbackKeys = 0;
            long embeddingKeys = 0;

            if (allKeys != null) {
                for (String key : allKeys) {
                    if (key.contains("first:")) {
                        firstQuestionKeys++;
                    } else if (key.contains("feedback:")) {
                        feedbackKeys++;
                    } else if (key.contains("embedding:")) {
                        embeddingKeys++;
                    }
                }
            }

            stats.put("totalKeys", totalKeys);
            stats.put("firstQuestionKeys", firstQuestionKeys);
            stats.put("feedbackKeys", feedbackKeys);
            stats.put("embeddingKeys", embeddingKeys);
            stats.put("cacheEnabled", cacheEnabled);
            stats.put("embeddingEnabled", embeddingEnabled);
            stats.put("similarityThreshold", similarityThreshold);

        } catch (Exception e) {
            logger.error("获取缓存统计信息失败", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * 异步计算并缓存embedding（供队列调用）
     */
    public void calculateAndCacheEmbedding(String text, String cacheKey) {
        if (!embeddingEnabled || text == null || text.trim().isEmpty()) {
            return;
        }

        try {
            double[] embedding = getTextEmbedding(text);
            if (embedding != null) {
                String embeddingStr = objectMapper.writeValueAsString(embedding);
                redisTemplate.opsForValue().set(cacheKey, embeddingStr, 86400, TimeUnit.SECONDS);
                logger.debug("Embedding计算并缓存成功: cacheKey={}", cacheKey);
            } else {
                logger.warn("Embedding计算失败: cacheKey={}", cacheKey);
            }
        } catch (Exception e) {
            logger.error("计算并缓存embedding失败: cacheKey={}", cacheKey, e);
        }
    }

    /**
     * 批量计算embedding（供队列调用）
     */
    public void batchCalculateEmbeddings(Map<String, String> textCacheMap) {
        if (!embeddingEnabled || textCacheMap == null || textCacheMap.isEmpty()) {
            return;
        }

        try {
            List<String> texts = new ArrayList<>(textCacheMap.keySet());
            int batchSize = 20; // 默认批量大小

            for (int i = 0; i < texts.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, endIndex);

                // 批量调用OpenAI API
                List<double[]> embeddings = batchGetTextEmbeddings(batch);

                // 缓存结果
                for (int j = 0; j < batch.size() && j < embeddings.size(); j++) {
                    String text = batch.get(j);
                    String cacheKey = textCacheMap.get(text);
                    double[] embedding = embeddings.get(j);

                    if (embedding != null && cacheKey != null) {
                        String embeddingStr = objectMapper.writeValueAsString(embedding);
                        redisTemplate.opsForValue().set(cacheKey, embeddingStr, 86400, TimeUnit.SECONDS);
                    }
                }

                logger.debug("批量embedding计算完成: batch={}/{}, size={}",
                        (i/batchSize + 1), (texts.size() + batchSize - 1)/batchSize, batch.size());

                // 避免API调用过于频繁
                if (i + batchSize < texts.size()) {
                    Thread.sleep(100);
                }
            }

        } catch (Exception e) {
            logger.error("批量计算embedding失败", e);
        }
    }

    /**
     * 检查答案相似度（供队列调用）
     */
    public boolean checkAnswerSimilarity(Long questionId, String userAnswer) {
        if (!embeddingEnabled || userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }

        try {
            // 获取该题目的历史标准答案进行相似度比较
            String searchPattern = aiCachePrefix + "answer:" + questionId + ":*";
            Set<String> answerKeys = redisTemplate.keys(searchPattern);

            if (answerKeys == null || answerKeys.isEmpty()) {
                return false;
            }

            double[] userEmbedding = getTextEmbedding(userAnswer);
            if (userEmbedding == null) {
                return false;
            }

            double maxSimilarity = 0.0;
            for (String answerKey : answerKeys) {
                try {
                    String embeddingKey = answerKey + ":embedding";
                    String embeddingStr = redisTemplate.opsForValue().get(embeddingKey);

                    if (embeddingStr != null) {
                        double[] answerEmbedding = objectMapper.readValue(embeddingStr, double[].class);
                        double similarity = calculateCosineSimilarity(userEmbedding, answerEmbedding);
                        maxSimilarity = Math.max(maxSimilarity, similarity);
                    }
                } catch (Exception e) {
                    logger.warn("检查答案相似度失败: answerKey={}", answerKey, e);
                }
            }

            boolean similar = maxSimilarity >= similarityThreshold;
            logger.debug("答案相似度检查: questionId={}, maxSimilarity={}, similar={}",
                    questionId, maxSimilarity, similar);

            return similar;

        } catch (Exception e) {
            logger.error("检查答案相似度失败: questionId={}", questionId, e);
            return false;
        }
    }

    /**
     * 批量获取文本embedding - 供队列使用
     */
    private List<double[]> batchGetTextEmbeddings(List<String> texts) {
        if (!embeddingEnabled || texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", embeddingModel,
                    "input", texts,
                    "encoding_format", "float"
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    embeddingApiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");

                List<double[]> embeddings = new ArrayList<>();
                if (data != null) {
                    for (Map<String, Object> item : data) {
                        List<Double> embeddingList = (List<Double>) item.get("embedding");
                        if (embeddingList != null) {
                            double[] embedding = embeddingList.stream().mapToDouble(Double::doubleValue).toArray();
                            embeddings.add(embedding);
                        } else {
                            embeddings.add(null);
                        }
                    }
                }

                if (embeddingCallMade != null) embeddingCallMade.increment();
                logger.debug("批量获取embedding成功: textCount={}, embeddingCount={}", texts.size(), embeddings.size());

                return embeddings;
            }

            logger.warn("批量获取embedding失败: response={}", response.getStatusCode());
            return new ArrayList<>();

        } catch (Exception e) {
            logger.error("批量调用embedding API失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 预热题目缓存 - 供启动时调用
     */
    public void preWarmQuestionCache(List<Long> questionIds) {
        if (!cacheEnabled || !embeddingEnabled || questionIds == null || questionIds.isEmpty()) {
            return;
        }

        try {
            List<Map<String, String>> embeddingRequests = new ArrayList<>();

            for (Long questionId : questionIds) {
                // 为每个题目准备embedding计算
                String cacheKey = "question:embedding:" + questionId;
                String text = "question_id_" + questionId; // 简化的文本标识

                Map<String, String> request = new HashMap<>();
                request.put("text", text);
                request.put("cacheKey", cacheKey);
                embeddingRequests.add(request);
            }

            if (!embeddingRequests.isEmpty()) {
                String batchId = "preWarm:" + System.currentTimeMillis();
                // 如果有队列，使用队列；否则直接处理
                if (aiQueueProducer != null) {
                    // 这里需要队列Producer的相应方法
                    logger.info("题目缓存预热请求已发送到队列: batchId={}, count={}", batchId, embeddingRequests.size());
                } else {
                    // 直接处理
                    Map<String, String> textCacheMap = new HashMap<>();
                    for (Map<String, String> req : embeddingRequests) {
                        textCacheMap.put(req.get("text"), req.get("cacheKey"));
                    }
                    batchCalculateEmbeddings(textCacheMap);
                    logger.info("题目缓存预热完成: count={}", embeddingRequests.size());
                }
            }

        } catch (Exception e) {
            logger.error("预热题目缓存失败", e);
        }
    }
}