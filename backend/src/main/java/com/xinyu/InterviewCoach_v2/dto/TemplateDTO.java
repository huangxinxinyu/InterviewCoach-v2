package com.xinyu.InterviewCoach_v2.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模板数据传输对象
 */
public class TemplateDTO {

    private Long id;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 255, message = "模板名称长度不能超过255个字符")
    private String name;

    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // 额外字段 - 解析后的模板结构
    private List<TemplateSection> sections;
    private Integer totalQuestionCount;

    public TemplateDTO() {}

    public TemplateDTO(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public TemplateDTO(Long id, String name, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 验证请求数据的有效性
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty();
    }

    /**
     * 获取处理后的模板名称（去除首尾空格）
     */
    public String getProcessedName() {
        return name != null ? name.trim() : null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TemplateSection> getSections() {
        return sections;
    }

    public void setSections(List<TemplateSection> sections) {
        this.sections = sections;
    }

    public Integer getTotalQuestionCount() {
        return totalQuestionCount;
    }

    public void setTotalQuestionCount(Integer totalQuestionCount) {
        this.totalQuestionCount = totalQuestionCount;
    }

    @Override
    public String toString() {
        return "TemplateDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", totalQuestionCount=" + totalQuestionCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TemplateDTO that = (TemplateDTO) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * 模板章节内部类
     */
    public static class TemplateSection {
        private String name;
        private List<Long> tagIds;
        private Integer questionCount;

        public TemplateSection() {}

        public TemplateSection(String name, List<Long> tagIds, Integer questionCount) {
            this.name = name;
            this.tagIds = tagIds;
            this.questionCount = questionCount;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Long> getTagIds() {
            return tagIds;
        }

        public void setTagIds(List<Long> tagIds) {
            this.tagIds = tagIds;
        }

        public Integer getQuestionCount() {
            return questionCount;
        }

        public void setQuestionCount(Integer questionCount) {
            this.questionCount = questionCount;
        }

        @Override
        public String toString() {
            return "TemplateSection{" +
                    "name='" + name + '\'' +
                    ", tagIds=" + tagIds +
                    ", questionCount=" + questionCount +
                    '}';
        }
    }
}