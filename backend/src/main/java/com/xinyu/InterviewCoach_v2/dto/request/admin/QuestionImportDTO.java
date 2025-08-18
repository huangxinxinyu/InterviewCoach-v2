package com.xinyu.InterviewCoach_v2.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class QuestionImportDTO {

    @NotBlank(message = "题目内容不能为空")
    @Size(min = 5, max = 1000, message = "题目内容长度必须在5-1000字符之间")
    private String text;

    private List<String> tags;

    // 新增：答案列表，支持一题多答案
    private List<String> answers;

    public QuestionImportDTO() {}

    public QuestionImportDTO(String text, List<String> tags) {
        this.text = text;
        this.tags = tags;
    }

    public QuestionImportDTO(String text, List<String> tags, List<String> answers) {
        this.text = text;
        this.tags = tags;
        this.answers = answers;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }
}