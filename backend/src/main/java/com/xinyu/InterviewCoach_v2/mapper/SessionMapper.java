package com.xinyu.InterviewCoach_v2.mapper;

import com.xinyu.InterviewCoach_v2.entity.Session;
import com.xinyu.InterviewCoach_v2.enums.SessionMode;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 会话数据访问层
 */
@Mapper
public interface SessionMapper {

    /**
     * 插入新会话
     */
    @Insert("INSERT INTO session (user_id, mode, expected_question_count, asked_question_count, " +
            "completed_question_count, started_at, is_active) " +
            "VALUES (#{userId}, #{mode}, #{expectedQuestionCount}, #{askedQuestionCount}, " +
            "#{completedQuestionCount}, #{startedAt}, #{isActive})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Session session);

    /**
     * 根据ID查询会话
     */
    @Select("SELECT id, user_id, mode, expected_question_count, asked_question_count, " +
            "completed_question_count, started_at, ended_at, is_active " +
            "FROM session WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "mode", column = "mode", javaType = SessionMode.class),
            @Result(property = "expectedQuestionCount", column = "expected_question_count"),
            @Result(property = "askedQuestionCount", column = "asked_question_count"),
            @Result(property = "completedQuestionCount", column = "completed_question_count"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "endedAt", column = "ended_at"),
            @Result(property = "isActive", column = "is_active")
    })
    Optional<Session> findById(Long id);

    /**
     * 根据用户ID查询活跃会话
     */
    @Select("SELECT id, user_id, mode, expected_question_count, asked_question_count, " +
            "completed_question_count, started_at, ended_at, is_active " +
            "FROM session WHERE user_id = #{userId} AND is_active = true " +
            "ORDER BY started_at DESC LIMIT 1")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "mode", column = "mode", javaType = SessionMode.class),
            @Result(property = "expectedQuestionCount", column = "expected_question_count"),
            @Result(property = "askedQuestionCount", column = "asked_question_count"),
            @Result(property = "completedQuestionCount", column = "completed_question_count"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "endedAt", column = "ended_at"),
            @Result(property = "isActive", column = "is_active")
    })
    Optional<Session> findActiveByUserId(Long userId);

    /**
     * 根据用户ID查询所有会话
     */
    @Select("SELECT id, user_id, mode, expected_question_count, asked_question_count, " +
            "completed_question_count, started_at, ended_at, is_active " +
            "FROM session WHERE user_id = #{userId} " +
            "ORDER BY started_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "mode", column = "mode", javaType = SessionMode.class),
            @Result(property = "expectedQuestionCount", column = "expected_question_count"),
            @Result(property = "askedQuestionCount", column = "asked_question_count"),
            @Result(property = "completedQuestionCount", column = "completed_question_count"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "endedAt", column = "ended_at"),
            @Result(property = "isActive", column = "is_active")
    })
    List<Session> findByUserId(Long userId);

    /**
     * 更新会话
     */
    @Update("UPDATE session SET expected_question_count = #{expectedQuestionCount}, " +
            "asked_question_count = #{askedQuestionCount}, " +
            "completed_question_count = #{completedQuestionCount}, " +
            "ended_at = #{endedAt}, is_active = #{isActive} " +
            "WHERE id = #{id}")
    int update(Session session);

    /**
     * 增加已提问题目数量
     */
    @Update("UPDATE session SET asked_question_count = asked_question_count + 1 " +
            "WHERE id = #{sessionId}")
    int incrementAskedQuestionCount(Long sessionId);

    /**
     * 增加已完成题目数量
     */
    @Update("UPDATE session SET completed_question_count = completed_question_count + 1 " +
            "WHERE id = #{sessionId}")
    int incrementCompletedQuestionCount(Long sessionId);

    /**
     * 结束会话
     */
    @Update("UPDATE session SET is_active = false, ended_at = NOW() " +
            "WHERE id = #{sessionId}")
    int endSession(Long sessionId);

    /**
     * 结束用户所有活跃会话
     */
    @Update("UPDATE session SET is_active = false, ended_at = NOW() " +
            "WHERE user_id = #{userId} AND is_active = true")
    int endAllActiveSessionsByUserId(Long userId);

    /**
     * 根据ID删除会话
     */
    @Delete("DELETE FROM session WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 统计用户会话总数
     */
    @Select("SELECT COUNT(*) FROM session WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    /**
     * 检查用户是否有活跃会话
     */
    @Select("SELECT COUNT(*) > 0 FROM session WHERE user_id = #{userId} AND is_active = true")
    boolean hasActiveSession(Long userId);
}