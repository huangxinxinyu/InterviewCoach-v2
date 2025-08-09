package com.xinyu.InterviewCoach_v2.mapper;

import com.xinyu.InterviewCoach_v2.entity.User;
import com.xinyu.InterviewCoach_v2.enums.UserRole;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper {

    /**
     * 插入新用户
     */
    @Insert("INSERT INTO user (email, password, role, created_at, updated_at) " +
            "VALUES (#{email}, #{password}, #{role}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 根据ID查询用户
     */
    @Select("SELECT id, email, password, role, created_at, updated_at FROM user WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "email", column = "email"),
            @Result(property = "password", column = "password"),
            @Result(property = "role", column = "role", javaType = UserRole.class),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<User> findById(Long id);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT id, email, password, role, created_at, updated_at FROM user WHERE email = #{email}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "email", column = "email"),
            @Result(property = "password", column = "password"),
            @Result(property = "role", column = "role", javaType = UserRole.class),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<User> findByEmail(String email);

    /**
     * 查询所有用户
     */
    @Select("SELECT id, email, password, role, created_at, updated_at FROM user ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "email", column = "email"),
            @Result(property = "password", column = "password"),
            @Result(property = "role", column = "role", javaType = UserRole.class),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<User> findAll();

    /**
     * 根据角色查询用户
     */
    @Select("SELECT id, email, password, role, created_at, updated_at FROM user WHERE role = #{role}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "email", column = "email"),
            @Result(property = "password", column = "password"),
            @Result(property = "role", column = "role", javaType = UserRole.class),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<User> findByRole(UserRole role);

    /**
     * 更新用户信息
     */
    @Update("UPDATE user SET email = #{email}, password = #{password}, role = #{role}, updated_at = NOW() WHERE id = #{id}")
    int update(User user);

    /**
     * 根据ID删除用户
     */
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 检查邮箱是否存在
     */
    @Select("SELECT COUNT(*) FROM user WHERE email = #{email}")
    boolean existsByEmail(String email);

    /**
     * 统计用户总数
     */
    @Select("SELECT COUNT(*) FROM user")
    long count();
}