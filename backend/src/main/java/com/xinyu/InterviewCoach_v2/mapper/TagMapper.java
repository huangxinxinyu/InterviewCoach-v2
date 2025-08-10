package com.xinyu.InterviewCoach_v2.mapper;

import com.xinyu.InterviewCoach_v2.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 标签数据访问层
 */
@Mapper
public interface TagMapper {

    /**
     * 插入新标签
     */
    @Insert("INSERT INTO tag (name, created_at, updated_at) " +
            "VALUES (#{name}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tag tag);

    /**
     * 根据ID查询标签
     */
    @Select("SELECT id, name, created_at, updated_at FROM tag WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<Tag> findById(Long id);

    /**
     * 根据名称查询标签
     */
    @Select("SELECT id, name, created_at, updated_at FROM tag WHERE name = #{name}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<Tag> findByName(String name);

    /**
     * 查询所有标签
     */
    @Select("SELECT id, name, created_at, updated_at FROM tag ORDER BY name ASC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Tag> findAll();

    /**
     * 根据关键词搜索标签
     */
    @Select("SELECT id, name, created_at, updated_at FROM tag " +
            "WHERE name LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY name ASC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Tag> findByKeyword(String keyword);

    /**
     * 分页查询标签
     */
    @Select("SELECT id, name, created_at, updated_at FROM tag " +
            "ORDER BY name ASC " +
            "LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Tag> findWithPagination(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * 更新标签
     */
    @Update("UPDATE tag SET name = #{name}, updated_at = NOW() WHERE id = #{id}")
    int update(Tag tag);

    /**
     * 根据ID删除标签
     */
    @Delete("DELETE FROM tag WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 统计标签总数
     */
    @Select("SELECT COUNT(*) FROM tag")
    long count();

    /**
     * 根据关键词统计标签数量
     */
    @Select("SELECT COUNT(*) FROM tag WHERE name LIKE CONCAT('%', #{keyword}, '%')")
    long countByKeyword(String keyword);

    /**
     * 检查标签名称是否已存在
     */
    @Select("SELECT COUNT(*) FROM tag WHERE name = #{name}")
    boolean existsByName(String name);

    /**
     * 获取最常用的标签（根据使用次数排序）
     */
    @Select("SELECT t.id, t.name, t.created_at, t.updated_at, COUNT(qt.question_id) as usage_count " +
            "FROM tag t " +
            "LEFT JOIN question_tag qt ON t.id = qt.tag_id " +
            "GROUP BY t.id, t.name, t.created_at, t.updated_at " +
            "ORDER BY usage_count DESC, t.name ASC " +
            "LIMIT #{limit}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Tag> findMostUsed(int limit);

    /**
     * 获取未使用的标签
     */
    @Select("SELECT t.id, t.name, t.created_at, t.updated_at " +
            "FROM tag t " +
            "LEFT JOIN question_tag qt ON t.id = qt.tag_id " +
            "WHERE qt.tag_id IS NULL " +
            "ORDER BY t.name ASC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Tag> findUnused();

    /**
     * 根据题目ID查询相关标签
     */
    @Select("SELECT t.id, t.name, t.created_at, t.updated_at " +
            "FROM tag t " +
            "INNER JOIN question_tag qt ON t.id = qt.tag_id " +
            "WHERE qt.question_id = #{questionId} " +
            "ORDER BY t.name ASC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Tag> findByQuestionId(Long questionId);
}