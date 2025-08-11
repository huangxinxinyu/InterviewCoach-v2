package com.xinyu.InterviewCoach_v2.mapper;

import com.xinyu.InterviewCoach_v2.entity.UserAttempt;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;
 /**
 * 用户答题尝试数据访问层
 */
@Mapper
public interface UserAttemptMapper {

    /**
     * 插入或更新用户答题尝试记录
     */
    @Insert("INSERT INTO user_attempt (user_id, question_id, attempt_number, updated_at) " +
            "VALUES (#{userId}, #{questionId}, #{attemptNumber}, #{updatedAt}) " +
            "ON DUPLICATE KEY UPDATE " +
            "attempt_number = attempt_number + 1, updated_at = NOW()")
    int insertOrUpdate(UserAttempt userAttempt);

    /**
     * 根据用户ID和题目ID查询尝试记录
     */
    @Select("SELECT user_id, question_id, attempt_number, updated_at " +
            "FROM user_attempt WHERE user_id = #{userId} AND question_id = #{questionId}")
    @Results({
            @Result(property = "userId", column = "user_id"),
            @Result(property = "questionId", column = "question_id"),
            @Result(property = "attemptNumber", column = "attempt_number"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<UserAttempt> findByUserIdAndQuestionId(@Param("userId") Long userId,
                                                    @Param("questionId") Long questionId);

    /**
     * 根据用户ID查询所有尝试记录
     */
    @Select("SELECT user_id, question_id, attempt_number, updated_at " +
            "FROM user_attempt WHERE user_id = #{userId} " +
            "ORDER BY updated_at DESC")
    @Results({
            @Result(property = "userId", column = "user_id"),
            @Result(property = "questionId", column = "question_id"),
            @Result(property = "attemptNumber", column = "attempt_number"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<UserAttempt> findByUserId(Long userId);

    /**
     * 获取用户从未尝试过的题目（根据标签）
     */
    @Select("SELECT q.id, q.text, q.created_at, q.updated_at " +
            "FROM question q " +
            "INNER JOIN question_tag qt ON q.id = qt.question_id " +
            "WHERE qt.tag_id = #{tagId} " +
            "AND q.id NOT IN ( " +
            "  SELECT question_id FROM user_attempt WHERE user_id = #{userId} " +
            ") " +
            "ORDER BY q.created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<com.xinyu.InterviewCoach_v2.entity.Question> findUntriedQuestionsByTagId(
            @Param("userId") Long userId, @Param("tagId") Long tagId);

    /**
     * 获取用户尝试次数最少的题目（根据标签）
     */
    @Select("SELECT q.id, q.text, q.created_at, q.updated_at, " +
            "COALESCE(ua.attempt_number, 0) as attempt_count " +
            "FROM question q " +
            "INNER JOIN question_tag qt ON q.id = qt.question_id " +
            "LEFT JOIN user_attempt ua ON q.id = ua.question_id AND ua.user_id = #{userId} " +
            "WHERE qt.tag_id = #{tagId} " +
            "ORDER BY attempt_count ASC, q.created_at DESC " +
            "LIMIT #{limit}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<com.xinyu.InterviewCoach_v2.entity.Question> findLeastAttemptedQuestionsByTagId(
            @Param("userId") Long userId, @Param("tagId") Long tagId, @Param("limit") int limit);

    /**
     * 记录用户答题尝试
     */
    @Insert("INSERT INTO user_attempt (user_id, question_id, attempt_number) " +
            "VALUES (#{userId}, #{questionId}, 1) " +
            "ON DUPLICATE KEY UPDATE " +
            "attempt_number = attempt_number + 1, updated_at = NOW()")
    int recordAttempt(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 统计用户总尝试次数
     */
    @Select("SELECT COUNT(*) FROM user_attempt WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    /**
     * 获取用户尝试过的题目总数
     */
    @Select("SELECT COUNT(DISTINCT question_id) FROM user_attempt WHERE user_id = #{userId}")
    long countDistinctQuestionsByUserId(Long userId);
}
