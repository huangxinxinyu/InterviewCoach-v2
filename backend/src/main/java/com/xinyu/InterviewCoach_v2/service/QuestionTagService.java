package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.QuestionDTO;
import com.xinyu.InterviewCoach_v2.dto.TagDTO;
import com.xinyu.InterviewCoach_v2.entity.Question;
import com.xinyu.InterviewCoach_v2.entity.Tag;
import com.xinyu.InterviewCoach_v2.mapper.QuestionMapper;
import com.xinyu.InterviewCoach_v2.mapper.QuestionTagMapper;
import com.xinyu.InterviewCoach_v2.mapper.TagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目标签关联业务逻辑层
 */
@Service
public class QuestionTagService {

    @Autowired
    private QuestionTagMapper questionTagMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private TagMapper tagMapper;

    /**
     * 为题目添加单个标签
     */
    @Transactional
    public boolean addTagToQuestion(Long questionId, Long tagId) {
        // 验证题目和标签是否存在
        if (!questionMapper.findById(questionId).isPresent()) {
            throw new RuntimeException("题目不存在");
        }
        if (!tagMapper.findById(tagId).isPresent()) {
            throw new RuntimeException("标签不存在");
        }

        // 检查关联是否已存在
        if (questionTagMapper.existsQuestionTag(questionId, tagId)) {
            throw new RuntimeException("题目已有该标签");
        }

        return questionTagMapper.addTagToQuestion(questionId, tagId) > 0;
    }

    /**
     * 从题目中移除单个标签
     */
    @Transactional
    public boolean removeTagFromQuestion(Long questionId, Long tagId) {
        // 检查关联是否存在
        if (!questionTagMapper.existsQuestionTag(questionId, tagId)) {
            throw new RuntimeException("题目没有该标签");
        }

        return questionTagMapper.removeTagFromQuestion(questionId, tagId) > 0;
    }

    /**
     * 批量为题目添加标签
     */
    @Transactional
    public boolean addTagsToQuestion(Long questionId, List<Long> tagIds) {
        // 验证题目是否存在
        if (!questionMapper.findById(questionId).isPresent()) {
            throw new RuntimeException("题目不存在");
        }

        // 验证所有标签是否存在
        for (Long tagId : tagIds) {
            if (!tagMapper.findById(tagId).isPresent()) {
                throw new RuntimeException("标签ID " + tagId + " 不存在");
            }
        }

        // 过滤掉已存在的关联
        List<Long> newTagIds = tagIds.stream()
                .filter(tagId -> !questionTagMapper.existsQuestionTag(questionId, tagId))
                .collect(Collectors.toList());

        if (newTagIds.isEmpty()) {
            throw new RuntimeException("所有标签都已关联到该题目");
        }

        return questionTagMapper.addTagsToQuestion(questionId, newTagIds) > 0;
    }

    /**
     * 批量从题目中移除标签
     */
    @Transactional
    public boolean removeTagsFromQuestion(Long questionId, List<Long> tagIds) {
        return questionTagMapper.removeTagsFromQuestion(questionId, tagIds) > 0;
    }

    /**
     * 移除题目的所有标签
     */
    @Transactional
    public boolean removeAllTagsFromQuestion(Long questionId) {
        return questionTagMapper.removeAllTagsFromQuestion(questionId) >= 0;
    }

    /**
     * 设置题目的标签（先清除所有标签，再添加新标签）
     */
    @Transactional
    public boolean setQuestionTags(Long questionId, List<Long> tagIds) {
        // 验证题目是否存在
        if (!questionMapper.findById(questionId).isPresent()) {
            throw new RuntimeException("题目不存在");
        }

        // 验证所有标签是否存在
        for (Long tagId : tagIds) {
            if (!tagMapper.findById(tagId).isPresent()) {
                throw new RuntimeException("标签ID " + tagId + " 不存在");
            }
        }

        // 先移除所有现有标签
        questionTagMapper.removeAllTagsFromQuestion(questionId);

        // 如果有新标签，则添加
        if (!tagIds.isEmpty()) {
            return questionTagMapper.addTagsToQuestion(questionId, tagIds) > 0;
        }

        return true;
    }

    /**
     * 通过标签名称为题目添加标签（如果标签不存在则创建）
     */
    @Transactional
    public boolean addTagToQuestionByName(Long questionId, String tagName) {
        // 验证题目是否存在
        if (!questionMapper.findById(questionId).isPresent()) {
            throw new RuntimeException("题目不存在");
        }

        if (tagName == null || tagName.trim().isEmpty()) {
            throw new RuntimeException("标签名称不能为空");
        }

        String processedTagName = tagName.trim().toLowerCase();

        // 查找或创建标签
        Tag tag = tagMapper.findByName(processedTagName)
                .orElseGet(() -> {
                    Tag newTag = new Tag(processedTagName);
                    tagMapper.insert(newTag);
                    return newTag;
                });

        // 检查关联是否已存在
        if (questionTagMapper.existsQuestionTag(questionId, tag.getId())) {
            throw new RuntimeException("题目已有该标签");
        }

        return questionTagMapper.addTagToQuestion(questionId, tag.getId()) > 0;
    }

    /**
     * 获取题目的所有标签
     */
    public List<TagDTO> getTagsByQuestionId(Long questionId) {
        return questionTagMapper.findTagsByQuestionId(questionId).stream()
                .map(this::convertTagToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取标签的所有题目
     */
    public List<QuestionDTO> getQuestionsByTagId(Long tagId) {
        return questionTagMapper.findQuestionsByTagId(tagId).stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据多个标签查询题目（AND关系）
     */
    public List<QuestionDTO> getQuestionsByAllTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        return questionTagMapper.findQuestionsByAllTagIds(tagIds).stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据多个标签查询题目（OR关系）
     */
    public List<QuestionDTO> getQuestionsByAnyTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        return questionTagMapper.findQuestionsByAnyTagIds(tagIds).stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据标签名称查询题目（支持模糊搜索）
     */
    public List<QuestionDTO> getQuestionsByTagName(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            return List.of();
        }
        return questionTagMapper.findQuestionsByTagName(tagName.trim().toLowerCase()).stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取最热门的标签
     */
    public List<TagDTO> getHotTags(int limit) {
        if (limit < 1) limit = 10;
        return questionTagMapper.findHotTags(limit).stream()
                .map(this::convertTagToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取没有关联任何题目的标签
     */
    public List<TagDTO> getOrphanTags() {
        return questionTagMapper.findOrphanTags().stream()
                .map(this::convertTagToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取没有任何标签的题目
     */
    public List<QuestionDTO> getUntaggedQuestions() {
        return questionTagMapper.findUntaggedQuestions().stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取题目的标签数量
     */
    public int getTagCountByQuestionId(Long questionId) {
        return questionTagMapper.getTagCountByQuestionId(questionId);
    }

    /**
     * 获取标签的题目数量
     */
    public int getQuestionCountByTagId(Long tagId) {
        return questionTagMapper.getQuestionCountByTagId(tagId);
    }

    /**
     * 统计关联关系总数
     */
    public long getQuestionTagRelationCount() {
        return questionTagMapper.countQuestionTagRelations();
    }

    /**
     * 检查题目是否已有某个标签
     */
    public boolean hasTag(Long questionId, Long tagId) {
        return questionTagMapper.existsQuestionTag(questionId, tagId);
    }

    /**
     * 清理孤立标签（删除没有关联任何题目的标签）
     */
    @Transactional
    public int cleanupOrphanTags() {
        List<Tag> orphanTags = questionTagMapper.findOrphanTags();
        int deletedCount = 0;
        for (Tag tag : orphanTags) {
            if (tagMapper.deleteById(tag.getId()) > 0) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    /**
     * 将Tag实体转换为TagDTO
     */
    private TagDTO convertTagToDTO(Tag tag) {
        return new TagDTO(
                tag.getId(),
                tag.getName(),
                tag.getCreatedAt(),
                tag.getUpdatedAt()
        );
    }

    /**
     * 将Question实体转换为QuestionDTO
     */
    private QuestionDTO convertQuestionToDTO(Question question) {
        return new QuestionDTO(
                question.getId(),
                question.getText(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}