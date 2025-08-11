package com.xinyu.InterviewCoach_v2.mapper;

import com.xinyu.InterviewCoach_v2.entity.Message;
import com.xinyu.InterviewCoach_v2.entity.UserAttempt;
import com.xinyu.InterviewCoach_v2.enums.MessageType;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 消息数据访问层
 */
@Mapper
public interface MessageMapper {

    /**
     * 插入新消息
     */
    @Insert("INSERT INTO message (session_id, type, text, created_at) " +
            "VALUES (#{sessionId}, #{type}, #{text}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message message);

    /**
     * 根据ID查询消息
     */
    @Select("SELECT id, session_id, type, text, created_at " +
            "FROM message WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "sessionId", column = "session_id"),
            @Result(property = "type", column = "type", javaType = MessageType.class),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at")
    })
    Optional<Message> findById(Long id);

    /**
     * 根据会话ID查询所有消息（按时间升序）
     */
    @Select("SELECT id, session_id, type, text, created_at " +
            "FROM message WHERE session_id = #{sessionId} " +
            "ORDER BY created_at ASC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "sessionId", column = "session_id"),
            @Result(property = "type", column = "type", javaType = MessageType.class),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<Message> findBySessionId(Long sessionId);

    /**
     * 获取会话的最后一条消息
     */
    @Select("SELECT id, session_id, type, text, created_at " +
            "FROM message WHERE session_id = #{sessionId} " +
            "ORDER BY created_at DESC LIMIT 1")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "sessionId", column = "session_id"),
            @Result(property = "type", column = "type", javaType = MessageType.class),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at")
    })
    Optional<Message> findLastBySessionId(Long sessionId);

    /**
     * 统计会话消息数量
     */
    @Select("SELECT COUNT(*) FROM message WHERE session_id = #{sessionId}")
    long countBySessionId(Long sessionId);

    /**
     * 统计会话中AI消息数量
     */
    @Select("SELECT COUNT(*) FROM message WHERE session_id = #{sessionId} AND type = 'ai'")
    long countAIMessagesBySessionId(Long sessionId);

    /**
     * 统计会话中用户消息数量
     */
    @Select("SELECT COUNT(*) FROM message WHERE session_id = #{sessionId} AND type = 'user'")
    long countUserMessagesBySessionId(Long sessionId);

    /**
     * 根据会话ID删除所有消息
     */
    @Delete("DELETE FROM message WHERE session_id = #{sessionId}")
    int deleteBySessionId(Long sessionId);
}
