package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.entity.Answer;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiErrorResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiSuccessResponseDTO;
import com.xinyu.InterviewCoach_v2.service.AnswerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 答案控制层
 */
@RestController
@RequestMapping("/api/answers")
@CrossOrigin(origins = "*")
public class AnswerController {

    @Autowired
    private AnswerService answerService;

    /**
     * 创建新答案（可选接口，让表看起来更完整）
     */
    @PostMapping
    public ResponseEntity<?> createAnswer(@Valid @RequestBody Answer answer) {
        try {
            Answer createdAnswer = answerService.createAnswer(answer);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiSuccessResponseDTO<>("答案创建成功", createdAnswer));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "CREATE_ANSWER_FAILED"));
        }
    }

    /**
     * 根据ID查询答案
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAnswerById(@PathVariable Long id) {
        Optional<Answer> answer = answerService.getAnswerById(id);
        if (answer.isPresent()) {
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(answer.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根据题目ID查询所有答案
     */
    @GetMapping("/question/{questionId}")
    public ResponseEntity<ApiSuccessResponseDTO<List<Answer>>> getAnswersByQuestionId(@PathVariable Long questionId) {
        List<Answer> answers = answerService.getAnswersByQuestionId(questionId);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(answers));
    }

    /**
     * 查询所有答案
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponseDTO<List<Answer>>> getAllAnswers() {
        List<Answer> answers = answerService.getAllAnswers();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(answers));
    }

    /**
     * 分页查询答案
     */
    @GetMapping("/page")
    public ResponseEntity<ApiSuccessResponseDTO<List<Answer>>> getAnswersWithPagination(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<Answer> answers = answerService.getAnswersWithPagination(page, size);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(answers));
    }

    /**
     * 根据关键词搜索答案
     */
    @GetMapping("/search")
    public ResponseEntity<ApiSuccessResponseDTO<List<Answer>>> searchAnswers(@RequestParam String keyword) {
        List<Answer> answers = answerService.searchAnswers(keyword);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(answers));
    }

    /**
     * 统计答案总数
     */
    @GetMapping("/count")
    public ResponseEntity<ApiSuccessResponseDTO<Integer>> getTotalAnswerCount() {
        int count = answerService.getTotalAnswerCount();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(count));
    }

    /**
     * 统计指定题目的答案数量
     */
    @GetMapping("/count/question/{questionId}")
    public ResponseEntity<ApiSuccessResponseDTO<Integer>> getAnswerCountByQuestionId(@PathVariable Long questionId) {
        int count = answerService.getAnswerCountByQuestionId(questionId);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(count));
    }

    /**
     * 更新答案（可选接口）
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAnswer(@PathVariable Long id, @RequestBody Answer answer) {
        try {
            Answer updatedAnswer = answerService.updateAnswer(id, answer);
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>("答案更新成功", updatedAnswer));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "UPDATE_ANSWER_FAILED"));
        }
    }

    /**
     * 删除答案（可选接口）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnswer(@PathVariable Long id) {
        try {
            boolean deleted = answerService.deleteAnswer(id);
            if (deleted) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("答案删除成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("删除答案失败", "DELETE_ANSWER_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "DELETE_ANSWER_ERROR"));
        }
    }

    /**
     * 根据题目ID删除所有答案（可选接口）
     */
    @DeleteMapping("/question/{questionId}")
    public ResponseEntity<?> deleteAnswersByQuestionId(@PathVariable Long questionId) {
        try {
            boolean deleted = answerService.deleteAnswersByQuestionId(questionId);
            if (deleted) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("题目相关答案删除成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("删除答案失败", "DELETE_ANSWERS_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "DELETE_ANSWERS_ERROR"));
        }
    }
}