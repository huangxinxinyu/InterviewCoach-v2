package com.xinyu.InterviewCoach_v2.service.cache;

import com.xinyu.InterviewCoach_v2.dto.core.SessionDTO;
import com.xinyu.InterviewCoach_v2.entity.Session;
import com.xinyu.InterviewCoach_v2.mapper.SessionMapper;
import com.xinyu.InterviewCoach_v2.util.DTOConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存预热服务
 * 应用启动时预热Redis缓存
 */
@Service
public class CacheWarmupService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupService.class);

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private RedisSessionManager redisSessionManager;

    @Autowired
    private DTOConverter dtoConverter;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        warmupSessionCache();
    }

    /**
     * 预热会话缓存
     * 将最近活跃的会话加载到Redis
     */
    public void warmupSessionCache() {
        try {
            logger.info("开始预热会话缓存...");

            // 查询最近24小时内的活跃会话
            List<Session> recentSessions = sessionMapper.findRecentActiveSessions();

            int warmedCount = 0;
            for (Session session : recentSessions) {
                try {
                    SessionDTO sessionDTO = dtoConverter.convertToSessionDTO(session);
                    redisSessionManager.cacheSession(sessionDTO);

                    // 如果是活跃会话，也缓存用户活跃会话映射
                    if (session.getIsActive()) {
                        redisSessionManager.cacheUserActiveSession(session.getUserId(), session.getId());
                    }

                    warmedCount++;
                } catch (Exception e) {
                    logger.warn("预热会话缓存失败: sessionId={}, error={}", session.getId(), e.getMessage());
                }
            }

            logger.info("会话缓存预热完成: 成功预热{}个会话", warmedCount);

        } catch (Exception e) {
            logger.error("缓存预热失败", e);
        }
    }

    /**
     * 手动触发缓存预热（可用于运维）
     */
    public void manualWarmup() {
        warmupSessionCache();
    }
}