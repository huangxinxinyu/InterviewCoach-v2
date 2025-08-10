package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.QuestionDTO;
import com.xinyu.InterviewCoach_v2.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 题目控制层
 */
@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*") // 允许跨域访问
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    /**
     * 创建新题目
     */
    @PostMapping
    public ResponseEntity<?> createQuestion(@RequestBody QuestionDTO questionDTO) {
        try {
            QuestionDTO createdQuestion = questionService.createQuestion(questionDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdQuestion);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 根据ID查询题目
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestionById(@PathVariable Long id) {
        Optional<QuestionDTO> question = questionService.getQuestionById(id);
        if (question.isPresent()) {
            return ResponseEntity.ok(question.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 查询所有题目
     */
    @GetMapping
    public ResponseEntity<List<QuestionDTO>> getAllQuestions() {
        List<QuestionDTO> questions = questionService.getAllQuestions();
        return ResponseEntity.ok(questions);
    }

    /**
     * 根据关键词搜索题目
     */
    @GetMapping("/search")
    public ResponseEntity<List<QuestionDTO>> searchQuestions(@RequestParam String keyword) {
        List<QuestionDTO> questions = questionService.searchQuestions(keyword);
        return ResponseEntity.ok(questions);
    }

    /**
     * 分页查询题目
     */
    @GetMapping("/page")
    public ResponseEntity<PageResponse<QuestionDTO>> getQuestionsWithPagination(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<QuestionDTO> questions = questionService.getQuestionsWithPagination(page, size);
        long totalCount = questionService.getQuestionCount();
        int totalPages = (int) Math.ceil((double) totalCount / size);

        PageResponse<QuestionDTO> response = new PageResponse<>(
                questions, page, size, totalCount, totalPages
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 更新题目
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long id, @RequestBody QuestionDTO questionDTO) {
        try {
            QuestionDTO updatedQuestion = questionService.updateQuestion(id, questionDTO);
            return ResponseEntity.ok(updatedQuestion);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
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
                return ResponseEntity.ok(new SuccessResponse("题目删除成功"));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("删除题目失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取题目总数
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getQuestionCount() {
        long count = questionService.getQuestionCount();
        return ResponseEntity.ok(count);
    }

    /**
     * 根据关键词统计题目数量
     */
    @GetMapping("/count/search")
    public ResponseEntity<Long> getQuestionCountByKeyword(@RequestParam String keyword) {
        long count = questionService.getQuestionCountByKeyword(keyword);
        return ResponseEntity.ok(count);
    }

    /**
     * 获取最新的题目
     */
    @GetMapping("/latest")
    public ResponseEntity<List<QuestionDTO>> getLatestQuestions(
            @RequestParam(defaultValue = "10") int limit) {
        List<QuestionDTO> questions = questionService.getLatestQuestions(limit);
        return ResponseEntity.ok(questions);
    }

    /**
     * 随机获取题目
     */
    @GetMapping("/random")
    public ResponseEntity<List<QuestionDTO>> getRandomQuestions(
            @RequestParam(defaultValue = "10") int limit) {
        List<QuestionDTO> questions = questionService.getRandomQuestions(limit);
        return ResponseEntity.ok(questions);
    }

    /**
     * 检查题目是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkQuestionExists(@RequestParam String text) {
        boolean exists = questionService.questionExists(text);
        return ResponseEntity.ok(exists);
    }

    /**
     * 分页响应DTO
     */
    public static class PageResponse<T> {
        private List<T> content;
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;

        public PageResponse(List<T> content, int currentPage, int pageSize, long totalElements, int totalPages) {
            this.content = content;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        // Getters and Setters
        public List<T> getContent() {
            return content;
        }

        public void setContent(List<T> content) {
            this.content = content;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
    }

    /**
     * 错误响应DTO
     */
    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    /**
     * 成功响应DTO
     */
    public static class SuccessResponse {
        private String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}