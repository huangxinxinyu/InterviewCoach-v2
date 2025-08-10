package com.xinyu.InterviewCoach_v2.mapper;

import com.xinyu.InterviewCoach_v2.entity.Question;
import com.xinyu.InterviewCoach_v2.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 题目标签关联数据访问层
 * 直接操作关联表，不创建实体类
 */
@Mapper
public interface QuestionTagMapper {

    /**
     * 为题目添加标签
     */
    @Insert("INSERT INTO question_tag (question_id, tag_id) VALUES (#{questionId}, #{tagId})")
    int addTagToQuestion(@Param("questionId") Long questionId, @Param("tagId") Long tagId);

    /**
     * 从题目中移除标签
     */
    @Delete("DELETE FROM question_tag WHERE question_id = #{questionId} AND tag_id = #{tagId}")
    int removeTagFromQuestion(@Param("questionId") Long questionId, @Param("tagId") Long tagId);

    /**
     * 移除题目的所有标签
     */
    @Delete("DELETE FROM question_tag WHERE question_id = #{questionId}")
    int removeAllTagsFromQuestion(Long questionId);

    /**
     * 移除标签的所有关联（当删除标签时使用）
     */
    @Delete("DELETE FROM question_tag WHERE tag_id = #{tagId}")
    int removeAllQuestionsFromTag(Long tagId);

    /**
     * 检查题目是否已有某个标签
     */
    @Select("SELECT COUNT(*) FROM question_tag WHERE question_id = #{questionId} AND tag_id = #{tagId}")
    boolean existsQuestionTag(@Param("questionId") Long questionId, @Param("tagId") Long tagId);

    /**
     * 获取题目的标签数量
     */
    @Select("SELECT COUNT(*) FROM question_tag WHERE question_id = #{questionId}")
    int getTagCountByQuestionId(Long questionId);

    /**
     * 获取标签的题目数量
     */
    @Select("SELECT COUNT(*) FROM question_tag WHERE tag_id = #{tagId}")
    int getQuestionCountByTagId(Long tagId);

    /**
     * 根据题目ID查询所有关联的标签
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
    List<Tag> findTagsByQuestionId(Long questionId);

    /**
     * 根据标签ID查询所有关联的题目
     */
    @Select("SELECT q.id, q.text, q.created_at, q.updated_at " +
            "FROM question q " +
            "INNER JOIN question_tag qt ON q.id = qt.question_id " +
            "WHERE qt.tag_id = #{tagId} " +
            "ORDER BY q.created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Question> findQuestionsByTagId(Long tagId);

    /**
     * 根据多个标签ID查询题目（AND关系 - 题目必须包含所有指定标签）
     */
    @Select("<script>" +
            "SELECT q.id, q.text, q.created_at, q.updated_at " +
            "FROM question q " +
            "WHERE q.id IN (" +
            "  SELECT qt.question_id " +
            "  FROM question_tag qt " +
            "  WHERE qt.tag_id IN " +
            "  <foreach item='tagId' collection='tagIds' open='(' separator=',' close=')'>" +
            "    #{tagId}" +
            "  </foreach>" +
            "  GROUP BY qt.question_id " +
            "  HAVING COUNT(DISTINCT qt.tag_id) = #{tagIds.size()}" +
            ") " +
            "ORDER BY q.created_at DESC" +
            "</script>")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Question> findQuestionsByAllTagIds(@Param("tagIds") List<Long> tagIds);

    /**
     * 根据多个标签ID查询题目（OR关系 - 题目包含任一指定标签）
     */
    @Select("<script>" +
            "SELECT DISTINCT q.id, q.text, q.created_at, q.updated_at " +
            "FROM question q " +
            "INNER JOIN question_tag qt ON q.id = qt.question_id " +
            "WHERE qt.tag_id IN " +
            "<foreach item='tagId' collection='tagIds' open='(' separator=',' close=')'>" +
            "  #{tagId}" +
            "</foreach>" +
            "ORDER BY q.created_at DESC" +
            "</script>")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Question> findQuestionsByAnyTagIds(@Param("tagIds") List<Long> tagIds);

    /**
     * 批量为题目添加标签
     */
    @Insert("<script>" +
            "INSERT INTO question_tag (question_id, tag_id) VALUES " +
            "<foreach item='tagId' collection='tagIds' separator=','>" +
            "  (#{questionId}, #{tagId})" +
            "</foreach>" +
            "</script>")
    int addTagsToQuestion(@Param("questionId") Long questionId, @Param("tagIds") List<Long> tagIds);

    /**
     * 批量从题目中移除标签
     */
    @Delete("<script>" +
            "DELETE FROM question_tag " +
            "WHERE question_id = #{questionId} AND tag_id IN " +
            "<foreach item='tagId' collection='tagIds' open='(' separator=',' close=')'>" +
            "  #{tagId}" +
            "</foreach>" +
            "</script>")
    int removeTagsFromQuestion(@Param("questionId") Long questionId, @Param("tagIds") List<Long> tagIds);

    /**
     * 获取最热门的标签（按关联题目数量排序）
     */
    @Select("SELECT t.id, t.name, t.created_at, t.updated_at, COUNT(qt.question_id) as question_count " +
            "FROM tag t " +
            "LEFT JOIN question_tag qt ON t.id = qt.tag_id " +
            "GROUP BY t.id, t.name, t.created_at, t.updated_at " +
            "ORDER BY question_count DESC, t.name ASC " +
            "LIMIT #{limit}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Tag> findHotTags(int limit);

    /**
     * 获取没有关联任何题目的标签
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
    List<Tag> findOrphanTags();

    /**
     * 获取没有任何标签的题目
     */
    @Select("SELECT q.id, q.text, q.created_at, q.updated_at " +
            "FROM question q " +
            "LEFT JOIN question_tag qt ON q.id = qt.question_id " +
            "WHERE qt.question_id IS NULL " +
            "ORDER BY q.created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Question> findUntaggedQuestions();

    /**
     * 统计关联关系总数
     */
    @Select("SELECT COUNT(*) FROM question_tag")
    long countQuestionTagRelations();

    /**
     * 根据标签名称查询题目（支持模糊搜索）
     */
    @Select("SELECT DISTINCT q.id, q.text, q.created_at, q.updated_at " +
            "FROM question q " +
            "INNER JOIN question_tag qt ON q.id = qt.question_id " +
            "INNER JOIN tag t ON qt.tag_id = t.id " +
            "WHERE t.name LIKE CONCAT('%', #{tagName}, '%') " +
            "ORDER BY q.created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "text", column = "text"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Question> findQuestionsByTagName(String tagName);
}