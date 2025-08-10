package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.TagDTO;
import com.xinyu.InterviewCoach_v2.entity.Tag;
import com.xinyu.InterviewCoach_v2.mapper.TagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 标签业务逻辑层
 */
@Service
public class TagService {

    @Autowired
    private TagMapper tagMapper;

    /**
     * 创建新标签
     */
    public TagDTO createTag(TagDTO tagDTO) {
        // 验证输入
        if (!tagDTO.isValid()) {
            throw new RuntimeException("标签名称不能为空且长度不能超过50个字符");
        }

        // 处理标签名称（统一为小写并去除空格）
        String processedName = tagDTO.getProcessedName();

        // 检查标签是否已存在
        if (tagMapper.existsByName(processedName)) {
            throw new RuntimeException("标签已存在");
        }

        Tag tag = new Tag();
        tag.setName(processedName);

        int result = tagMapper.insert(tag);
        if (result > 0) {
            return convertToDTO(tag);
        } else {
            throw new RuntimeException("创建标签失败");
        }
    }

    /**
     * 根据ID查询标签
     */
    public Optional<TagDTO> getTagById(Long id) {
        return tagMapper.findById(id).map(this::convertToDTO);
    }

    /**
     * 根据名称查询标签
     */
    public Optional<TagDTO> getTagByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return tagMapper.findByName(name.trim().toLowerCase()).map(this::convertToDTO);
    }

    /**
     * 查询所有标签
     */
    public List<TagDTO> getAllTags() {
        return tagMapper.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据关键词搜索标签
     */
    public List<TagDTO> searchTags(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTags();
        }
        return tagMapper.findByKeyword(keyword.trim().toLowerCase()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询标签
     */
    public List<TagDTO> getTagsWithPagination(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        int offset = (page - 1) * size;
        return tagMapper.findWithPagination(size, offset).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新标签
     */
    public TagDTO updateTag(Long id, TagDTO tagDTO) {
        // 验证输入
        if (!tagDTO.isValid()) {
            throw new RuntimeException("标签名称不能为空且长度不能超过50个字符");
        }

        Optional<Tag> existingTag = tagMapper.findById(id);
        if (existingTag.isEmpty()) {
            throw new RuntimeException("标签不存在");
        }

        String processedName = tagDTO.getProcessedName();

        // 检查新名称是否与其他标签重复（排除自己）
        Tag existing = existingTag.get();
        if (!existing.getName().equals(processedName) && tagMapper.existsByName(processedName)) {
            throw new RuntimeException("标签名称已存在");
        }

        Tag tag = existing;
        tag.setName(processedName);

        int result = tagMapper.update(tag);
        if (result > 0) {
            return convertToDTO(tag);
        } else {
            throw new RuntimeException("更新标签失败");
        }
    }

    /**
     * 删除标签
     */
    public boolean deleteTag(Long id) {
        if (!tagMapper.findById(id).isPresent()) {
            throw new RuntimeException("标签不存在");
        }
        return tagMapper.deleteById(id) > 0;
    }

    /**
     * 获取标签总数
     */
    public long getTagCount() {
        return tagMapper.count();
    }

    /**
     * 根据关键词统计标签数量
     */
    public long getTagCountByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getTagCount();
        }
        return tagMapper.countByKeyword(keyword.trim().toLowerCase());
    }

    /**
     * 检查标签是否存在
     */
    public boolean tagExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return tagMapper.existsByName(name.trim().toLowerCase());
    }

    /**
     * 获取最常用的标签
     */
    public List<TagDTO> getMostUsedTags(int limit) {
        if (limit < 1) limit = 10;
        return tagMapper.findMostUsed(limit).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取未使用的标签
     */
    public List<TagDTO> getUnusedTags() {
        return tagMapper.findUnused().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据题目ID查询相关标签
     */
    public List<TagDTO> getTagsByQuestionId(Long questionId) {
        return tagMapper.findByQuestionId(questionId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建或获取标签（如果不存在则创建）
     */
    public TagDTO createOrGetTag(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("标签名称不能为空");
        }

        String processedName = name.trim().toLowerCase();
        Optional<TagDTO> existingTag = getTagByName(processedName);

        if (existingTag.isPresent()) {
            return existingTag.get();
        } else {
            TagDTO newTag = new TagDTO(processedName);
            return createTag(newTag);
        }
    }

    /**
     * 批量创建或获取标签
     */
    public List<TagDTO> createOrGetTags(List<String> names) {
        return names.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(this::createOrGetTag)
                .collect(Collectors.toList());
    }

    /**
     * 将Tag实体转换为TagDTO
     */
    private TagDTO convertToDTO(Tag tag) {
        return new TagDTO(
                tag.getId(),
                tag.getName(),
                tag.getCreatedAt(),
                tag.getUpdatedAt()
        );
    }
}