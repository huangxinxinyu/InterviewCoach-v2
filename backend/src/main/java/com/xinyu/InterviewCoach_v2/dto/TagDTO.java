package com.xinyu.InterviewCoach_v2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 标签数据传输对象 - 用于请求和响应
 */
public class TagDTO {

    private Long id;

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称长度不能超过50个字符")
    private String name;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TagDTO() {}

    public TagDTO(String name) {
        this.name = name;
    }

    public TagDTO(Long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    @Override
    public String toString() {
        return "TagDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TagDTO that = (TagDTO) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * 验证请求数据的有效性
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() && name.trim().length() <= 50;
    }

    /**
     * 获取处理后的标签名称（去除首尾空格并转换为小写）
     */
    public String getProcessedName() {
        return name != null ? name.trim().toLowerCase() : null;
    }

    /**
     * 获取显示用的标签名称（保持原始大小写，但去除空格）
     */
    public String getDisplayName() {
        return name != null ? name.trim() : null;
    }
}