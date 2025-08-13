package com.xinyu.InterviewCoach_v2.mapper;

import com.xinyu.InterviewCoach_v2.entity.Template;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 模板数据访问层
 */
@Mapper
public interface TemplateMapper {

    /**
     * 插入新模板
     */
    @Insert("INSERT INTO template (name, content, created_at, updated_at) " +
            "VALUES (#{name}, #{content}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Template template);

    /**
     * 根据ID查询模板
     */
    @Select("SELECT id, name, content, created_at, updated_at FROM template WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "content", column = "content"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<Template> findById(Long id);

    /**
     * 根据名称查询模板
     */
    @Select("SELECT id, name, content, created_at, updated_at FROM template WHERE name = #{name}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "content", column = "content"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<Template> findByName(String name);

    /**
     * 查询所有模板
     */
    @Select("SELECT id, name, content, created_at, updated_at FROM template ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "content", column = "content"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Template> findAll();

    /**
     * 根据关键词搜索模板
     */
    @Select("SELECT id, name, content, created_at, updated_at FROM template " +
            "WHERE name LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "content", column = "content"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Template> findByKeyword(String keyword);

    /**
     * 分页查询模板
     */
    @Select("SELECT id, name, content, created_at, updated_at FROM template " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "content", column = "content"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Template> findWithPagination(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * 更新模板
     */
    @Update("UPDATE template SET name = #{name}, content = #{content}, updated_at = NOW() WHERE id = #{id}")
    int update(Template template);

    /**
     * 根据ID删除模板
     */
    @Delete("DELETE FROM template WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 统计模板总数
     */
    @Select("SELECT COUNT(*) FROM template")
    long count();

    /**
     * 根据关键词统计模板数量
     */
    @Select("SELECT COUNT(*) FROM template WHERE name LIKE CONCAT('%', #{keyword}, '%')")
    long countByKeyword(String keyword);

    /**
     * 检查模板名称是否已存在
     */
    @Select("SELECT COUNT(*) FROM template WHERE name = #{name}")
    boolean existsByName(String name);

    /**
     * 获取最新的N个模板
     */
    @Select("SELECT id, name, content, created_at, updated_at FROM template " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "content", column = "content"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Template> findLatest(int limit);
}