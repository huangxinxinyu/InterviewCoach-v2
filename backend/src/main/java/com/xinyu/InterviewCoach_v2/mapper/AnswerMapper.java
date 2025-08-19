package com.xinyu.InterviewCoach_v2.mapper;

import com.xinyu.InterviewCoach_v2.entity.Answer;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 答案数据访问层
 */
@Mapper
public interface AnswerMapper {

    /**
     * 插入新答案
     */
    @Insert("INSERT INTO answer (question_id, text, created_at, updated_at) " +
            "VALUES (#{questionId}, #{text}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Answer answer);

    /**
     * 根据ID查询答案
     */
    @Select("SELECT id, question_id, text, created_at, updated_at FROM answer WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "questionId", column = "question_id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<Answer> findById(Long id);

    /**
     * 根据题目ID查询所有答案
     */
    @Select("SELECT id, question_id, text, created_at, updated_at FROM answer " +
            "WHERE question_id = #{questionId} ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "questionId", column = "question_id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Answer> findByQuestionId(Long questionId);

    /**
     * 查询所有答案
     */
    @Select("SELECT id, question_id, text, created_at, updated_at FROM answer ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "questionId", column = "question_id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Answer> findAll();

    /**
     * 分页查询答案
     */
    @Select("SELECT id, question_id, text, created_at, updated_at FROM answer " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "questionId", column = "question_id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Answer> findWithPagination(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * 根据关键词搜索答案
     */
    @Select("SELECT id, question_id, text, created_at, updated_at FROM answer " +
            "WHERE text LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "questionId", column = "question_id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Answer> findByKeyword(String keyword);

    /**
     * 统计答案总数
     */
    @Select("SELECT COUNT(*) FROM answer")
    int count();

    /**
     * 统计指定题目的答案数量
     */
    @Select("SELECT COUNT(*) FROM answer WHERE question_id = #{questionId}")
    int countByQuestionId(Long questionId);

    /**
     * 更新答案
     */
    @Update("UPDATE answer SET text = #{text}, updated_at = NOW() WHERE id = #{id}")
    int update(Answer answer);

    /**
     * 根据ID删除答案
     */
    @Delete("DELETE FROM answer WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据题目ID删除所有答案
     */
    @Delete("DELETE FROM answer WHERE question_id = #{questionId}")
    int deleteByQuestionId(Long questionId);
}