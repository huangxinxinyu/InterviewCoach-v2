package com.xinyu.InterviewCoach_v2.mapper;

import com.xinyu.InterviewCoach_v2.entity.Question;
import com.xinyu.InterviewCoach_v2.entity.QuestionSet;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 题集数据访问层
 */
@Mapper
public interface QuestionSetMapper {

    /**
     * 创建新题集
     */
    @Insert("INSERT INTO question_set (name, description, created_at, updated_at) " +
            "VALUES (#{name}, #{description}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionSet questionSet);

    /**
     * 根据ID查询题集
     */
    @Select("SELECT id, name, description, created_at, updated_at " +
            "FROM question_set WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "description", column = "description"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<QuestionSet> findById(Long id);

    /**
     * 查询所有题集
     */
    @Select("SELECT id, name, description, created_at, updated_at " +
            "FROM question_set ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "description", column = "description"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<QuestionSet> findAll();

    /**
     * 向题集添加题目
     */
    @Insert("INSERT INTO question_set_item (question_id, question_set_id) " +
            "VALUES (#{questionId}, #{questionSetId})")
    int addQuestionToSet(@Param("questionSetId") Long questionSetId,
                         @Param("questionId") Long questionId);

    /**
     * 批量向题集添加题目
     */
    @Insert("<script>" +
            "INSERT INTO question_set_item (question_id, question_set_id) VALUES " +
            "<foreach item='questionId' collection='questionIds' separator=','>" +
            "  (#{questionId}, #{questionSetId})" +
            "</foreach>" +
            "</script>")
    int addQuestionsToSet(@Param("questionSetId") Long questionSetId,
                          @Param("questionIds") List<Long> questionIds);

    /**
     * 从题集移除题目
     */
    @Delete("DELETE FROM question_set_item " +
            "WHERE question_set_id = #{questionSetId} AND question_id = #{questionId}")
    int removeQuestionFromSet(@Param("questionSetId") Long questionSetId,
                              @Param("questionId") Long questionId);

    /**
     * 清空题集中的所有题目
     */
    @Delete("DELETE FROM question_set_item WHERE question_set_id = #{questionSetId}")
    int clearQuestionSet(Long questionSetId);

    /**
     * 获取题集中的所有题目
     */
    @Select("SELECT q.id, q.text, q.created_at, q.updated_at " +
            "FROM question q " +
            "INNER JOIN question_set_item qsi ON q.id = qsi.question_id " +
            "WHERE qsi.question_set_id = #{questionSetId} " +
            "ORDER BY qsi.question_id")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Question> findQuestionsBySetId(Long questionSetId);

    /**
     * 获取题集中的题目数量
     */
    @Select("SELECT COUNT(*) FROM question_set_item WHERE question_set_id = #{questionSetId}")
    int getQuestionCountBySetId(Long questionSetId);

    /**
     * 用户收藏题集
     */
    @Insert("INSERT INTO question_set_collection (question_set_id, user_id, created_at) " +
            "VALUES (#{questionSetId}, #{userId}, NOW())")
    int collectQuestionSet(@Param("questionSetId") Long questionSetId,
                           @Param("userId") Long userId);

    /**
     * 用户取消收藏题集
     */
    @Delete("DELETE FROM question_set_collection " +
            "WHERE question_set_id = #{questionSetId} AND user_id = #{userId}")
    int uncollectQuestionSet(@Param("questionSetId") Long questionSetId,
                             @Param("userId") Long userId);

    /**
     * 检查用户是否已收藏题集
     */
    @Select("SELECT COUNT(*) > 0 FROM question_set_collection " +
            "WHERE question_set_id = #{questionSetId} AND user_id = #{userId}")
    boolean isCollectedByUser(@Param("questionSetId") Long questionSetId,
                              @Param("userId") Long userId);

    /**
     * 获取用户收藏的所有题集
     */
    @Select("SELECT qs.id, qs.name, qs.description, qs.created_at, qs.updated_at " +
            "FROM question_set qs " +
            "INNER JOIN question_set_collection qsc ON qs.id = qsc.question_set_id " +
            "WHERE qsc.user_id = #{userId} " +
            "ORDER BY qsc.created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "description", column = "description"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<QuestionSet> findCollectedSetsByUserId(Long userId);

    /**
     * 获取题集被收藏的次数
     */
    @Select("SELECT COUNT(*) FROM question_set_collection WHERE question_set_id = #{questionSetId}")
    int getCollectionCountBySetId(Long questionSetId);

    /**
     * 更新题集信息
     */
    @Update("UPDATE question_set SET name = #{name}, description = #{description}, " +
            "updated_at = NOW() WHERE id = #{id}")
    int update(QuestionSet questionSet);

    /**
     * 删除题集
     */
    @Delete("DELETE FROM question_set WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 检查题目是否在题集中
     */
    @Select("SELECT COUNT(*) > 0 FROM question_set_item " +
            "WHERE question_set_id = #{questionSetId} AND question_id = #{questionId}")
    boolean isQuestionInSet(@Param("questionSetId") Long questionSetId,
                            @Param("questionId") Long questionId);
}