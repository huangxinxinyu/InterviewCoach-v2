package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.QuestionDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiErrorResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiSuccessResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.PageResponseDTO;
import com.xinyu.InterviewCoach_v2.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 题目控制层 - 重构后使用统一的DTO
 */
@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    /**
     * 创建新题目
     */
    @PostMapping
    public ResponseEntity<?> createQuestion(@Valid @RequestBody QuestionDTO questionDTO) {
        try {
            QuestionDTO createdQuestion = questionService.createQuestion(questionDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiSuccessResponseDTO<>("题目创建成功", createdQuestion));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "CREATE_QUESTION_FAILED"));
        }
    }

    /**
     * 根据ID查询题目
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestionById(@PathVariable Long id) {
        Optional<QuestionDTO> question = questionService.getQuestionById(id);
        if (question.isPresent()) {
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(question.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 查询所有题目
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponseDTO<List<QuestionDTO>>> getAllQuestions() {
        List<QuestionDTO> questions = questionService.getAllQuestions();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(questions));
    }

    /**
     * 根据关键词搜索题目
     */
    @GetMapping("/search")
    public ResponseEntity<ApiSuccessResponseDTO<List<QuestionDTO>>> searchQuestions(@RequestParam String keyword) {
        List<QuestionDTO> questions = questionService.searchQuestions(keyword);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(questions));
    }

    /**
     * 分页查询题目
     */
    @GetMapping("/page")
    public ResponseEntity<ApiSuccessResponseDTO<PageResponseDTO<QuestionDTO>>> getQuestionsWithPagination(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<QuestionDTO> questions = questionService.getQuestionsWithPagination(page, size);
        long totalCount = questionService.getQuestionCount();
        int totalPages = (int) Math.ceil((double) totalCount / size);

        PageResponseDTO<QuestionDTO> pageResponse = PageResponseDTO.<QuestionDTO>builder()
                .content(questions)
                .currentPage(page)
                .pageSize(size)
                .totalElements(totalCount)
                .totalPages(totalPages);

        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(pageResponse));
    }

    /**
     * 更新题目
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionDTO questionDTO) {
        try {
            QuestionDTO updatedQuestion = questionService.updateQuestion(id, questionDTO);
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>("题目更新成功", updatedQuestion));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "UPDATE_QUESTION_FAILED"));
        }
    }

    /**
     * 删除题目
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        try {
            boolean deleted = questionService.deleteQuestion(id);
            if (deleted) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("题目删除成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("删除题目失败", "DELETE_QUESTION_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "DELETE_QUESTION_ERROR"));
        }
    }

    /**
     * 获取题目总数
     */
    @GetMapping("/count")
    public ResponseEntity<ApiSuccessResponseDTO<Long>> getQuestionCount() {
        long count = questionService.getQuestionCount();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(count));
    }

    /**
     * 根据关键词统计题目数量
     */
    @GetMapping("/count/search")
    public ResponseEntity<ApiSuccessResponseDTO<Long>> getQuestionCountByKeyword(@RequestParam String keyword) {
        long count = questionService.getQuestionCountByKeyword(keyword);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(count));
    }

    /**
     * 获取最新的题目
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiSuccessResponseDTO<List<QuestionDTO>>> getLatestQuestions(
            @RequestParam(defaultValue = "10") int limit) {
        List<QuestionDTO> questions = questionService.getLatestQuestions(limit);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(questions));
    }

    /**
     * 随机获取题目
     */
    @GetMapping("/random")
    public ResponseEntity<ApiSuccessResponseDTO<List<QuestionDTO>>> getRandomQuestions(
            @RequestParam(defaultValue = "10") int limit) {
        List<QuestionDTO> questions = questionService.getRandomQuestions(limit);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(questions));
    }

    /**
     * 检查题目是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<ApiSuccessResponseDTO<Boolean>> checkQuestionExists(@RequestParam String text) {
        boolean exists = questionService.questionExists(text);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(exists));
    }
}