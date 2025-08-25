package com.xinyu.InterviewCoach_v2.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinyu.InterviewCoach_v2.dto.core.MessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis消息缓存管理器
 * 缓存会话的消息历史，减少数据库查询
 */
@Component
public class RedisMessageManager {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageManager.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${cache.message.ttl:3600}")
    private long messageTtl;

    @Value("${cache.message.prefix:message:}")
    private String messagePrefix;

    /**
     * 缓存会话消息列表
     */
    public void cacheSessionMessages(Long sessionId, List<MessageDTO> messages) {
        try {
            String key = buildSessionMessagesKey(sessionId);
            String value = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, value, messageTtl, TimeUnit.SECONDS);
            logger.debug("缓存会话消息: sessionId={}, messageCount={}", sessionId, messages.size());
        } catch (JsonProcessingException e) {
            logger.error("消息序列化失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 从缓存获取会话消息列表
     */
    public Optional<List<MessageDTO>> getSessionMessages(Long sessionId) {
        try {
            String key = buildSessionMessagesKey(sessionId);
            String value = redisTemplate.opsForValue().get(key);

            if (value != null) {
                List<MessageDTO> messages = objectMapper.readValue(value,
                        new TypeReference<List<MessageDTO>>() {});
                logger.debug("从缓存获取会话消息: sessionId={}, messageCount={}", sessionId, messages.size());
                return Optional.of(messages);
            }

            return Optional.empty();

        } catch (JsonProcessingException e) {
            logger.error("消息反序列化失败: sessionId={}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * 添加新消息到缓存
     */
    public void addMessageToCache(Long sessionId, MessageDTO message) {
        Optional<List<MessageDTO>> existingMessages = getSessionMessages(sessionId);
        if (existingMessages.isPresent()) {
            List<MessageDTO> messages = existingMessages.get();
            messages.add(message);
            cacheSessionMessages(sessionId, messages);
            logger.debug("添加消息到缓存: sessionId={}, messageId={}", sessionId, message.getId());
        }
    }

    /**
     * 删除会话消息缓存
     */
    public void removeSessionMessages(Long sessionId) {
        String key = buildSessionMessagesKey(sessionId);
        redisTemplate.delete(key);
        logger.debug("删除会话消息缓存: sessionId={}", sessionId);
    }

    /**
     * 刷新消息缓存TTL
     */
    public void refreshMessageTtl(Long sessionId) {
        String key = buildSessionMessagesKey(sessionId);
        redisTemplate.expire(key, messageTtl, TimeUnit.SECONDS);
    }

    /**
     * 构建会话消息缓存key
     */
    private String buildSessionMessagesKey(Long sessionId) {
        return messagePrefix + "session:" + sessionId;
    }
}