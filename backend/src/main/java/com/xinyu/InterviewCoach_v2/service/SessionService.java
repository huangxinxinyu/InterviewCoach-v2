package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.core.SessionDTO;
import com.xinyu.InterviewCoach_v2.entity.Session;
import com.xinyu.InterviewCoach_v2.enums.SessionMode;
import com.xinyu.InterviewCoach_v2.mapper.SessionMapper;
import com.xinyu.InterviewCoach_v2.service.cache.RedisSessionManager;
import com.xinyu.InterviewCoach_v2.util.DTOConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 会话业务逻辑层 - 集成Redis缓存
 */
@Service
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private RedisSessionManager redisSessionManager;

    @Autowired
    private DTOConverter dtoConverter;

    /**
     * 创建新会话
     */
    @Transactional
    public SessionDTO createSession(Long userId, SessionMode mode, Integer expectedQuestionCount) {
        // 先结束用户所有活跃会话
        // sessionMapper.endAllActiveSessionsByUserId(userId);

        // 创建新会话
        Session session = new Session(userId, mode, expectedQuestionCount);

        int result = sessionMapper.insert(session);
        if (result > 0) {
            SessionDTO sessionDTO = dtoConverter.convertToSessionDTO(session);

            // 缓存新创建的会话
            redisSessionManager.cacheSession(sessionDTO);
            // 缓存用户活跃会话
            redisSessionManager.cacheUserActiveSession(userId, sessionDTO.getId());

            logger.info("创建会话成功并缓存: sessionId={}, userId={}", sessionDTO.getId(), userId);
            return sessionDTO;
        } else {
            throw new RuntimeException("创建会话失败");
        }
    }

    /**
     * 根据ID查询会话 - 优先从缓存读取
     */
    public Optional<SessionDTO> getSessionById(Long id) {
        // 1. 先从Redis缓存获取
        Optional<SessionDTO> cachedSession = redisSessionManager.getSession(id);
        if (cachedSession.isPresent()) {
            // 刷新TTL
            redisSessionManager.refreshSessionTtl(id);
            return cachedSession;
        }

        // 2. 缓存未命中，从数据库查询
        Optional<Session> dbSession = sessionMapper.findById(id);
        if (dbSession.isPresent()) {
            SessionDTO sessionDTO = dtoConverter.convertToSessionDTO(dbSession.get());
            // 回填缓存
            redisSessionManager.cacheSession(sessionDTO);
            logger.debug("会话数据回填缓存: sessionId={}", id);
            return Optional.of(sessionDTO);
        }

        return Optional.empty();
    }

    /**
     * 根据用户ID查询活跃会话 - 优先从缓存读取
     */
    public Optional<SessionDTO> getActiveSessionByUserId(Long userId) {
        // 1. 先从缓存获取用户活跃会话ID
        Optional<Long> cachedSessionId = redisSessionManager.getUserActiveSessionId(userId);
        if (cachedSessionId.isPresent()) {
            // 然后获取会话详情（这里会走上面的getSessionById缓存逻辑）
            Optional<SessionDTO> session = getSessionById(cachedSessionId.get());
            if (session.isPresent() && session.get().getIsActive()) {
                return session;
            } else {
                // 缓存的会话ID无效，清除缓存
                redisSessionManager.removeUserActiveSession(userId);
            }
        }

        // 2. 缓存未命中，从数据库查询
        Optional<Session> dbSession = sessionMapper.findActiveByUserId(userId);
        if (dbSession.isPresent()) {
            SessionDTO sessionDTO = dtoConverter.convertToSessionDTO(dbSession.get());
            // 回填缓存
            redisSessionManager.cacheSession(sessionDTO);
            redisSessionManager.cacheUserActiveSession(userId, sessionDTO.getId());
            logger.debug("活跃会话数据回填缓存: userId={}, sessionId={}", userId, sessionDTO.getId());
            return Optional.of(sessionDTO);
        }

        return Optional.empty();
    }

    /**
     * 根据用户ID查询所有会话
     */
    public List<SessionDTO> getSessionsByUserId(Long userId) {
        // 批量查询暂不缓存，直接查数据库
        return sessionMapper.findByUserId(userId).stream()
                .map(dtoConverter::convertToSessionDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新会话 - 更新缓存
     */
    @Transactional
    public SessionDTO updateSession(SessionDTO sessionDTO) {
        Optional<Session> existingSession = sessionMapper.findById(sessionDTO.getId());
        if (existingSession.isEmpty()) {
            throw new RuntimeException("会话不存在");
        }

        Session session = existingSession.get();
        session.setExpectedQuestionCount(sessionDTO.getExpectedQuestionCount());
        session.setAskedQuestionCount(sessionDTO.getAskedQuestionCount());
        session.setCompletedQuestionCount(sessionDTO.getCompletedQuestionCount());
        session.setEndedAt(sessionDTO.getEndedAt());
        session.setIsActive(sessionDTO.getIsActive());

        int result = sessionMapper.update(session);
        if (result > 0) {
            SessionDTO updatedSession = dtoConverter.convertToSessionDTO(session);
            // 更新缓存
            redisSessionManager.cacheSession(updatedSession);

            // 如果会话已结束，清除用户活跃会话缓存
            if (!updatedSession.getIsActive()) {
                redisSessionManager.removeUserActiveSession(session.getUserId());
            }

            logger.debug("更新会话并同步缓存: sessionId={}", updatedSession.getId());
            return updatedSession;
        } else {
            throw new RuntimeException("更新会话失败");
        }
    }

    /**
     * 增加已提问题目数量 - 同步缓存
     */
    @Transactional
    public boolean incrementAskedQuestionCount(Long sessionId) {
        boolean success = sessionMapper.incrementAskedQuestionCount(sessionId) > 0;
        if (success) {
            // 从数据库重新加载并更新缓存
            Optional<Session> updatedSession = sessionMapper.findById(sessionId);
            if (updatedSession.isPresent()) {
                SessionDTO sessionDTO = dtoConverter.convertToSessionDTO(updatedSession.get());
                redisSessionManager.cacheSession(sessionDTO);
                logger.debug("增加提问数量并更新缓存: sessionId={}", sessionId);
            }
        }
        return success;
    }

    /**
     * 增加已完成题目数量 - 同步缓存
     */
    @Transactional
    public boolean incrementCompletedQuestionCount(Long sessionId) {
        boolean success = sessionMapper.incrementCompletedQuestionCount(sessionId) > 0;
        if (success) {
            // 从数据库重新加载并更新缓存
            Optional<Session> updatedSession = sessionMapper.findById(sessionId);
            if (updatedSession.isPresent()) {
                SessionDTO sessionDTO = dtoConverter.convertToSessionDTO(updatedSession.get());
                redisSessionManager.cacheSession(sessionDTO);
                logger.debug("增加完成数量并更新缓存: sessionId={}", sessionId);
            }
        }
        return success;
    }

    /**
     * 结束会话 - 清理缓存
     */
    @Transactional
    public boolean endSession(Long sessionId) {
        // 获取会话信息用于清理用户活跃会话缓存
        Optional<SessionDTO> session = getSessionById(sessionId);

        boolean success = sessionMapper.endSession(sessionId) > 0;
        if (success) {
            // 清理缓存
            redisSessionManager.removeSession(sessionId);

            if (session.isPresent()) {
                redisSessionManager.removeUserActiveSession(session.get().getUserId());
            }

            logger.info("结束会话并清理缓存: sessionId={}", sessionId);
        }
        return success;
    }

    /**
     * 结束用户所有活跃会话 - 清理缓存
     */
    @Transactional
    public boolean endAllActiveSessionsByUserId(Long userId) {
        boolean success = sessionMapper.endAllActiveSessionsByUserId(userId) >= 0;
        if (success) {
            // 清理用户活跃会话缓存
            redisSessionManager.removeUserActiveSession(userId);
            logger.info("结束用户所有活跃会话并清理缓存: userId={}", userId);
        }
        return success;
    }

    /**
     * 删除会话 - 清理缓存
     */
    @Transactional
    public boolean deleteSession(Long id) {
        if (!sessionMapper.findById(id).isPresent()) {
            throw new RuntimeException("会话不存在");
        }

        boolean success = sessionMapper.deleteById(id) > 0;
        if (success) {
            // 清理缓存
            redisSessionManager.removeSession(id);
            logger.info("删除会话并清理缓存: sessionId={}", id);
        }
        return success;
    }

    /**
     * 统计用户会话总数
     */
    public long getSessionCountByUserId(Long userId) {
        return sessionMapper.countByUserId(userId);
    }

    /**
     * 检查用户是否有活跃会话 - 优先从缓存查询
     */
    public boolean hasActiveSession(Long userId) {
        // 先检查缓存
        Optional<Long> cachedSessionId = redisSessionManager.getUserActiveSessionId(userId);
        if (cachedSessionId.isPresent()) {
            // 验证缓存的会话是否真的活跃
            Optional<SessionDTO> session = getSessionById(cachedSessionId.get());
            if (session.isPresent() && session.get().getIsActive()) {
                return true;
            } else {
                // 缓存不一致，清理
                redisSessionManager.removeUserActiveSession(userId);
            }
        }

        // 从数据库查询
        return sessionMapper.hasActiveSession(userId);
    }

    /**
     * 验证会话所有权 - 优先从缓存验证
     */
    public boolean validateSessionOwnership(Long sessionId, Long userId) {
        Optional<SessionDTO> session = getSessionById(sessionId); // 会走缓存逻辑
        return session.isPresent() && session.get().getUserId().equals(userId);
    }

    /**
     * 检查会话是否已完成 - 优先从缓存查询
     */
    public boolean isSessionCompleted(Long sessionId) {
        Optional<SessionDTO> session = getSessionById(sessionId); // 会走缓存逻辑
        return session.isPresent() && session.get().getEndedAt() != null;
    }

    /**
     * 检查会话是否活跃 - 优先从缓存查询
     */
    public boolean isSessionActive(Long sessionId) {
        Optional<SessionDTO> session = getSessionById(sessionId); // 会走缓存逻辑
        return session.isPresent() && session.get().getIsActive();
    }
}