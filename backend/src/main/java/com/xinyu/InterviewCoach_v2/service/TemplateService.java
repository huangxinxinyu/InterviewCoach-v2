package com.xinyu.InterviewCoach_v2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinyu.InterviewCoach_v2.dto.TemplateDTO;
import com.xinyu.InterviewCoach_v2.entity.Template;
import com.xinyu.InterviewCoach_v2.mapper.TemplateMapper;
import com.xinyu.InterviewCoach_v2.util.DTOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 模板业务逻辑层
 */
@Service
public class TemplateService {

    @Autowired
    private TemplateMapper templateMapper;

    @Autowired
    private DTOConverter dtoConverter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建新模板
     */
    @Transactional
    public TemplateDTO createTemplate(TemplateDTO templateDTO) {
        // 验证输入
        if (!templateDTO.isValid()) {
            throw new RuntimeException("模板名称不能为空");
        }

        // 检查模板名称是否已存在
        String processedName = templateDTO.getProcessedName();
        if (templateMapper.existsByName(processedName)) {
            throw new RuntimeException("模板名称已存在");
        }

        Template template = new Template();
        template.setName(templateDTO.getName());
        template.setContent(templateDTO.getContent());
        template.setName(processedName);

        int result = templateMapper.insert(template);
        if (result > 0) {
            return dtoConverter.convertToTemplateDTO(template);
        } else {
            throw new RuntimeException("创建模板失败");
        }
    }

    /**
     * 根据ID查询模板
     */
    public Optional<TemplateDTO> getTemplateById(Long id) {
        return templateMapper.findById(id)
                .map(dtoConverter::convertToTemplateDTO);
    }

    /**
     * 根据名称查询模板
     */
    public Optional<TemplateDTO> getTemplateByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return templateMapper.findByName(name.trim())
                .map((dtoConverter::convertToTemplateDTO));
    }

    /**
     * 查询所有模板
     */
    public List<TemplateDTO> getAllTemplates() {
        List<Template> templates = templateMapper.findAll();
        return templates.stream()
                .map((dtoConverter::convertToTemplateDTO))
                .collect(Collectors.toList());
    }

    /**
     * 根据关键词搜索模板
     */
    public List<TemplateDTO> searchTemplates(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTemplates();
        }
        List<Template> templates = templateMapper.findByKeyword(keyword.trim());
        return templates.stream()
                .map((dtoConverter::convertToTemplateDTO))
                .collect(Collectors.toList());
    }

    /**
     * 分页查询模板
     */
    public List<TemplateDTO> getTemplatesWithPagination(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        int offset = (page - 1) * size;
        List<Template> templates = templateMapper.findWithPagination(size, offset);
        return templates.stream()
                .map((dtoConverter::convertToTemplateDTO))
                .collect(Collectors.toList());
    }

    /**
     * 更新模板
     */
    @Transactional
    public TemplateDTO updateTemplate(Long id, TemplateDTO templateDTO) {
        // 验证输入
        if (!templateDTO.isValid()) {
            throw new RuntimeException("模板名称不能为空");
        }

        Optional<Template> existingTemplate = templateMapper.findById(id);
        if (existingTemplate.isEmpty()) {
            throw new RuntimeException("模板不存在");
        }

        String processedName = templateDTO.getProcessedName();

        // 检查新名称是否与其他模板重复（排除自己）
        Template existing = existingTemplate.get();
        if (!existing.getName().equals(processedName) && templateMapper.existsByName(processedName)) {
            throw new RuntimeException("模板名称已存在");
        }

        // 更新模板信息
        existing.setName(processedName);
        existing.setContent(templateDTO.getContent());

        int result = templateMapper.update(existing);
        if (result > 0) {
            return dtoConverter.convertToTemplateDTO(existing);
        } else {
            throw new RuntimeException("更新模板失败");
        }
    }

    /**
     * 删除模板
     */
    @Transactional
    public boolean deleteTemplate(Long id) {
        if (!templateMapper.findById(id).isPresent()) {
            throw new RuntimeException("模板不存在");
        }
        return templateMapper.deleteById(id) > 0;
    }

    /**
     * 获取模板总数
     */
    public long getTemplateCount() {
        return templateMapper.count();
    }

    /**
     * 根据关键词统计模板数量
     */
    public long getTemplateCountByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getTemplateCount();
        }
        return templateMapper.countByKeyword(keyword.trim());
    }

    /**
     * 检查模板是否存在
     */
    public boolean templateExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return templateMapper.existsByName(name.trim());
    }

    /**
     * 获取最新的模板
     */
    public List<TemplateDTO> getLatestTemplates(int limit) {
        if (limit < 1) limit = 10;
        List<Template> templates = templateMapper.findLatest(limit);
        return templates.stream()
                .map((dtoConverter::convertToTemplateDTO))
                .collect(Collectors.toList());
    }

    /**
     * 解析模板内容获取结构化信息
     */
    public TemplateDTO parseTemplateContent(Long templateId) {
        Optional<Template> templateOpt = templateMapper.findById(templateId);
        if (templateOpt.isEmpty()) {
            throw new RuntimeException("模板不存在");
        }

        Template template = templateOpt.get();
        TemplateDTO dto = dtoConverter.convertToTemplateDTO(template);

        try {
            // 解析JSON内容
            Map<String, Object> contentMap = objectMapper.readValue(template.getContent(), new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> sectionsData = (List<Map<String, Object>>) contentMap.get("sections");

            if (sectionsData != null) {
                List<TemplateDTO.TemplateSection> sections = sectionsData.stream()
                        .map(sectionData -> {
                            String name = (String) sectionData.get("name");
                            List<Integer> tagIdInts = (List<Integer>) sectionData.get("tagIds");
                            Integer questionCount = (Integer) sectionData.get("questionCount");

                            List<Long> tagIds = tagIdInts.stream()
                                    .map(Integer::longValue)
                                    .collect(Collectors.toList());

                            return new TemplateDTO.TemplateSection(name, tagIds, questionCount);
                        })
                        .collect(Collectors.toList());

                dto.setSections(sections);

                // 计算总题目数量
                int totalCount = sections.stream()
                        .mapToInt(TemplateDTO.TemplateSection::getQuestionCount)
                        .sum();
                dto.setTotalQuestionCount(totalCount);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException("模板内容格式错误: " + e.getMessage());
        }

        return dto;
    }
}