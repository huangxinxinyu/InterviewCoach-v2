package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.QuestionSetDTO;
import com.xinyu.InterviewCoach_v2.service.QuestionSetService;
import com.xinyu.InterviewCoach_v2.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 题集控制层 - 简化版本，主要用于脚本管理
 */
@RestController
@RequestMapping("/api/question-sets")
@CrossOrigin(origins = "*")
public class QuestionSetController {

    @Autowired
    private QuestionSetService questionSetService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建题集（支持同时添加题目）
     * POST /api/question-sets
     * Body: {
     *   "name": "题集名称",
     *   "description": "描述",
     *   "questionIds": [1, 2, 3]  // 可选
     * }
     */
    @PostMapping
    public ResponseEntity<?> createQuestionSet(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            List<Integer> questionIdInts = (List<Integer>) request.get("questionIds");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "题集名称不能为空"));
            }

            List<Long> questionIds = null;
            if (questionIdInts != null) {
                questionIds = questionIdInts.stream().map(Integer::longValue).toList();
            }

            QuestionSetDTO result = questionSetService.createQuestionSet(name, description, questionIds);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取所有题集
     */
    @GetMapping
    public ResponseEntity<List<QuestionSetDTO>> getAllQuestionSets(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<QuestionSetDTO> questionSets = questionSetService.getAllQuestionSets(userId);
        return ResponseEntity.ok(questionSets);
    }

    /**
     * 根据ID获取题集详情（包含题目列表）
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestionSet(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        Optional<QuestionSetDTO> questionSet = questionSetService.getQuestionSetById(id, userId);

        if (questionSet.isPresent()) {
            return ResponseEntity.ok(questionSet.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 更新题集基本信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuestionSet(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "题集名称不能为空"));
            }

            QuestionSetDTO result = questionSetService.updateQuestionSet(id, name, description);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除题集
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestionSet(@PathVariable Long id) {
        try {
            boolean deleted = questionSetService.deleteQuestionSet(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "题集删除成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "删除失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 向题集添加题目
     * POST /api/question-sets/{id}/questions
     * Body: {"questionIds": [1, 2, 3]}
     */
    @PostMapping("/{id}/questions")
    public ResponseEntity<?> addQuestionsToSet(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> questionIdInts = (List<Integer>) request.get("questionIds");

            if (questionIdInts == null || questionIdInts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "题目ID列表不能为空"));
            }

            List<Long> questionIds = questionIdInts.stream().map(Integer::longValue).toList();
            boolean success = questionSetService.addQuestionsToSet(id, questionIds);

            if (success) {
                return ResponseEntity.ok(Map.of("message", "题目添加成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "添加失败"));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 从题集移除题目
     */
    @DeleteMapping("/{id}/questions/{questionId}")
    public ResponseEntity<?> removeQuestionFromSet(@PathVariable Long id, @PathVariable Long questionId) {
        try {
            boolean success = questionSetService.removeQuestionFromSet(id, questionId);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "题目移除成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "移除失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 设置题集内容（覆盖所有题目）
     * PUT /api/question-sets/{id}/questions
     * Body: {"questionIds": [1, 2, 3]}
     */
    @PutMapping("/{id}/questions")
    public ResponseEntity<?> setQuestionSetContent(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> questionIdInts = (List<Integer>) request.get("questionIds");

            List<Long> questionIds = null;
            if (questionIdInts != null) {
                questionIds = questionIdInts.stream().map(Integer::longValue).toList();
            }

            boolean success = questionSetService.setQuestionSetContent(id, questionIds);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "题集内容设置成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "设置失败"));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 用户收藏题集
     */
    @PostMapping("/{id}/collect")
    public ResponseEntity<?> collectQuestionSet(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "用户未认证"));
            }

            boolean success = questionSetService.collectQuestionSet(id, userId);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "收藏成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "收藏失败"));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 用户取消收藏题集
     */
    @DeleteMapping("/{id}/collect")
    public ResponseEntity<?> uncollectQuestionSet(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "用户未认证"));
            }

            boolean success = questionSetService.uncollectQuestionSet(id, userId);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "取消收藏成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "取消收藏失败"));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取用户收藏的题集
     */
    @GetMapping("/collections")
    public ResponseEntity<?> getUserCollectedSets(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "用户未认证"));
            }

            List<QuestionSetDTO> collections = questionSetService.getUserCollectedSets(userId);
            return ResponseEntity.ok(collections);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取题集的题目ID列表（用于面试配置）
     */
    @GetMapping("/{id}/question-ids")
    public ResponseEntity<?> getQuestionIds(@PathVariable Long id) {
        try {
            List<Long> questionIds = questionSetService.getQuestionIdsBySetId(id);
            return ResponseEntity.ok(Map.of("questionIds", questionIds));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                return jwtUtil.getUserIdFromToken(token);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}