package com.xinyu.InterviewCoach_v2.dto.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class BatchImportRequestDTO {

    @NotNull(message = "题目列表不能为空")
    @NotEmpty(message = "题目列表不能为空")
    @Valid
    private List<QuestionImportDTO> questions;

    public BatchImportRequestDTO() {}

    public List<QuestionImportDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionImportDTO> questions) {
        this.questions = questions;
    }
}