package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.mapper.UserAttemptMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户答题尝试业务逻辑层
 */
@Service
class UserAttemptService {

    @Autowired
    private UserAttemptMapper userAttemptMapper;

    /**
     * 记录或更新用户答题尝试
     */
    @Transactional
    public boolean recordAttempt(Long userId, Long questionId) {
        return userAttemptMapper.recordAttempt(userId, questionId) > 0;
    }

    /**
     * 获取用户对某题目的尝试次数
     */
    public int getAttemptNumber(Long userId, Long questionId) {
        return userAttemptMapper.findByUserIdAndQuestionId(userId, questionId)
                .map(attempt -> attempt.getAttemptNumber())
                .orElse(0);
    }

    /**
     * 获取用户总尝试次数
     */
    public long getTotalAttemptsByUserId(Long userId) {
        return userAttemptMapper.countByUserId(userId);
    }

    /**
     * 获取用户尝试过的题目总数
     */
    public long getDistinctQuestionCountByUserId(Long userId) {
        return userAttemptMapper.countDistinctQuestionsByUserId(userId);
    }
}