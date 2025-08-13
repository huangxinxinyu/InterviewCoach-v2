package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.core.SessionDTO;
import com.xinyu.InterviewCoach_v2.entity.Session;
import com.xinyu.InterviewCoach_v2.enums.SessionMode;
import com.xinyu.InterviewCoach_v2.mapper.SessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 会话业务逻辑层
 */
@Service
public class SessionService {

    @Autowired
    private SessionMapper sessionMapper;

    /**
     * 创建新会话
     */
    @Transactional
    public SessionDTO createSession(Long userId, SessionMode mode, Integer expectedQuestionCount) {
        // 先结束用户所有活跃会话
        sessionMapper.endAllActiveSessionsByUserId(userId);

        // 创建新会话
        Session session = new Session(userId, mode, expectedQuestionCount);

        int result = sessionMapper.insert(session);
        if (result > 0) {
            return convertToDTO(session);
        } else {
            throw new RuntimeException("创建会话失败");
        }
    }

    /**
     * 根据ID查询会话
     */
    public Optional<SessionDTO> getSessionById(Long id) {
        return sessionMapper.findById(id).map(this::convertToDTO);
    }

    /**
     * 根据用户ID查询活跃会话
     */
    public Optional<SessionDTO> getActiveSessionByUserId(Long userId) {
        return sessionMapper.findActiveByUserId(userId).map(this::convertToDTO);
    }

    /**
     * 根据用户ID查询所有会话
     */
    public List<SessionDTO> getSessionsByUserId(Long userId) {
        return sessionMapper.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新会话
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
            return convertToDTO(session);
        } else {
            throw new RuntimeException("更新会话失败");
        }
    }

    /**
     * 增加已提问题目数量
     */
    @Transactional
    public boolean incrementAskedQuestionCount(Long sessionId) {
        return sessionMapper.incrementAskedQuestionCount(sessionId) > 0;
    }

    /**
     * 增加已完成题目数量
     */
    @Transactional
    public boolean incrementCompletedQuestionCount(Long sessionId) {
        return sessionMapper.incrementCompletedQuestionCount(sessionId) > 0;
    }

    /**
     * 结束会话
     */
    @Transactional
    public boolean endSession(Long sessionId) {
        return sessionMapper.endSession(sessionId) > 0;
    }

    /**
     * 结束用户所有活跃会话
     */
    @Transactional
    public boolean endAllActiveSessionsByUserId(Long userId) {
        return sessionMapper.endAllActiveSessionsByUserId(userId) >= 0;
    }

    /**
     * 删除会话
     */
    @Transactional
    public boolean deleteSession(Long id) {
        if (!sessionMapper.findById(id).isPresent()) {
            throw new RuntimeException("会话不存在");
        }
        return sessionMapper.deleteById(id) > 0;
    }

    /**
     * 统计用户会话总数
     */
    public long getSessionCountByUserId(Long userId) {
        return sessionMapper.countByUserId(userId);
    }

    /**
     * 检查用户是否有活跃会话
     */
    public boolean hasActiveSession(Long userId) {
        return sessionMapper.hasActiveSession(userId);
    }

    /**
     * 验证会话所有权
     */
    public boolean validateSessionOwnership(Long sessionId, Long userId) {
        Optional<Session> session = sessionMapper.findById(sessionId);
        return session.isPresent() && session.get().getUserId().equals(userId);
    }

    /**
     * 检查会话是否已完成
     */
    public boolean isSessionCompleted(Long sessionId) {
        Optional<Session> session = sessionMapper.findById(sessionId);
        return session.isPresent() && session.get().isCompleted();
    }

    /**
     * 检查会话是否活跃
     */
    public boolean isSessionActive(Long sessionId) {
        Optional<Session> session = sessionMapper.findById(sessionId);
        return session.isPresent() && session.get().getIsActive();
    }

    /**
     * 将Session实体转换为SessionDTO
     */
    private SessionDTO convertToDTO(Session session) {
        return new SessionDTO(
                session.getId(),
                session.getUserId(),
                session.getMode(),
                session.getExpectedQuestionCount(),
                session.getAskedQuestionCount(),
                session.getCompletedQuestionCount(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getIsActive()
        );
    }
}