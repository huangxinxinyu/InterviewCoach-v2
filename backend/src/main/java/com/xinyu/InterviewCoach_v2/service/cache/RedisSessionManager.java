package com.xinyu.InterviewCoach_v2.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinyu.InterviewCoach_v2.dto.core.SessionDTO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.TimeUnit;
import java.util.Optional;

/**
 * Redis会话管理器
 * 负责会话数据的缓存操作
 */
@Component
public class RedisSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(RedisSessionManager.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${cache.session.ttl:1800}")
    private long sessionTtl;

    @Value("${cache.session.prefix:session:}")
    private String sessionPrefix;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private Counter cacheHit;
    private Counter cacheMiss;

    @PostConstruct
    private void initMetrics() {
        if (meterRegistry != null) {
            cacheHit = Counter.builder("session.cache.hit").register(meterRegistry);
            cacheMiss = Counter.builder("session.cache.miss").register(meterRegistry);
        }
    }

    /**
     * 缓存会话数据
     */
    public void cacheSession(SessionDTO session) {
        try {
            String key = buildSessionKey(session.getId());
            String value = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, value, sessionTtl, TimeUnit.SECONDS);
            logger.debug("会话缓存成功: sessionId={}", session.getId());
        } catch (JsonProcessingException e) {
            logger.error("会话序列化失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 从缓存获取会话
     */
    public Optional<SessionDTO> getSession(Long sessionId) {
        try {
            String key = buildSessionKey(sessionId);
            String value = redisTemplate.opsForValue().get(key);

            if (value != null) {
                SessionDTO session = objectMapper.readValue(value, SessionDTO.class);
                logger.debug("从缓存获取会话: sessionId={}", sessionId);
                return Optional.of(session);
            }

            logger.debug("缓存中未找到会话: sessionId={}", sessionId);
            return Optional.empty();

        } catch (JsonProcessingException e) {
            logger.error("会话反序列化失败: sessionId={}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * 删除会话缓存
     */
    public void removeSession(Long sessionId) {
        String key = buildSessionKey(sessionId);
        redisTemplate.delete(key);
        logger.debug("删除会话缓存: sessionId={}", sessionId);
    }

    /**
     * 刷新会话TTL
     */
    public void refreshSessionTtl(Long sessionId) {
        String key = buildSessionKey(sessionId);
        redisTemplate.expire(key, sessionTtl, TimeUnit.SECONDS);
        logger.debug("刷新会话TTL: sessionId={}", sessionId);
    }

    /**
     * 检查会话是否在缓存中
     */
    public boolean existsInCache(Long sessionId) {
        String key = buildSessionKey(sessionId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 缓存用户活跃会话ID
     */
    public void cacheUserActiveSession(Long userId, Long sessionId) {
        String key = buildUserActiveSessionKey(userId);
        redisTemplate.opsForValue().set(key, sessionId.toString(), sessionTtl, TimeUnit.SECONDS);
        logger.debug("缓存用户活跃会话: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 获取用户活跃会话ID
     */
    public Optional<Long> getUserActiveSessionId(Long userId) {
        String key = buildUserActiveSessionKey(userId);
        String sessionIdStr = redisTemplate.opsForValue().get(key);

        if (sessionIdStr != null) {
            try {
                Long sessionId = Long.parseLong(sessionIdStr);
                logger.debug("从缓存获取用户活跃会话: userId={}, sessionId={}", userId, sessionId);
                return Optional.of(sessionId);
            } catch (NumberFormatException e) {
                logger.warn("用户活跃会话ID格式错误: userId={}, value={}", userId, sessionIdStr);
                redisTemplate.delete(key);
            }
        }

        return Optional.empty();
    }

    /**
     * 清除用户活跃会话缓存
     */
    public void removeUserActiveSession(Long userId) {
        String key = buildUserActiveSessionKey(userId);
        redisTemplate.delete(key);
        logger.debug("清除用户活跃会话缓存: userId={}", userId);
    }

    /**
     * 构建会话缓存key
     */
    private String buildSessionKey(Long sessionId) {
        return sessionPrefix + sessionId;
    }

    /**
     * 构建用户活跃会话key
     */
    private String buildUserActiveSessionKey(Long userId) {
        return "user_active_session:" + userId;
    }
}