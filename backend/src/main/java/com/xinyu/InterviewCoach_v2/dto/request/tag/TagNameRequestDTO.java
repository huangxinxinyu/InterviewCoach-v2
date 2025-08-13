package com.xinyu.InterviewCoach_v2.dto.request.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 标签名称请求DTO
 */
public class TagNameRequestDTO {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称长度不能超过50个字符")
    private String name;

    public TagNameRequestDTO() {}

    public TagNameRequestDTO(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取处理后的标签名称（去除空格并转换为小写）
     */
    public String getProcessedName() {
        return name != null ? name.trim().toLowerCase() : null;
    }

    @Override
    public String toString() {
        return "TagNameRequestDTO{" +
                "name='" + name + '\'' +
                '}';
    }
}