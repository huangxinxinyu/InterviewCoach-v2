package com.xinyu.InterviewCoach_v2.mapper;

import com.xinyu.InterviewCoach_v2.entity.Question;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 题目数据访问层
 */
@Mapper
public interface QuestionMapper {

    /**
     * 插入新题目
     */
    @Insert("INSERT INTO question (text, created_at, updated_at) " +
            "VALUES (#{text}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Question question);

    /**
     * 根据ID查询题目
     */
    @Select("SELECT id, text, created_at, updated_at FROM question WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<Question> findById(Long id);

    /**
     * 查询所有题目
     */
    @Select("SELECT id, text, created_at, updated_at FROM question ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Question> findAll();

    /**
     * 根据关键词搜索题目
     */
    @Select("SELECT id, text, created_at, updated_at FROM question " +
            "WHERE text LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Question> findByKeyword(String keyword);

    /**
     * 分页查询题目
     */
    @Select("SELECT id, text, created_at, updated_at FROM question " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Question> findWithPagination(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * 更新题目
     */
    @Update("UPDATE question SET text = #{text}, updated_at = NOW() WHERE id = #{id}")
    int update(Question question);

    /**
     * 根据ID删除题目
     */
    @Delete("DELETE FROM question WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 统计题目总数
     */
    @Select("SELECT COUNT(*) FROM question")
    long count();

    /**
     * 根据关键词统计题目数量
     */
    @Select("SELECT COUNT(*) FROM question WHERE text LIKE CONCAT('%', #{keyword}, '%')")
    long countByKeyword(String keyword);

    /**
     * 检查题目文本是否已存在（用于防重复）
     */
    @Select("SELECT COUNT(*) FROM question WHERE text = #{text}")
    boolean existsByText(String text);

    /**
     * 获取最新的N个题目
     */
    @Select("SELECT id, text, created_at, updated_at FROM question " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Question> findLatest(int limit);

    /**
     * 随机获取N个题目
     */
    @Select("SELECT id, text, created_at, updated_at FROM question " +
            "ORDER BY RAND() " +
            "LIMIT #{limit}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Question> findRandom(int limit);
}