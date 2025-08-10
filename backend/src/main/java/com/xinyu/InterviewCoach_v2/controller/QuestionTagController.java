package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.QuestionDTO;
import com.xinyu.InterviewCoach_v2.dto.TagDTO;
import com.xinyu.InterviewCoach_v2.service.QuestionTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目标签关联控制层
 */
@RestController
@RequestMapping("/api/question-tags")
@CrossOrigin(origins = "*") // 允许跨域访问
public class QuestionTagController {

    @Autowired
    private QuestionTagService questionTagService;

    /**
     * 为题目添加单个标签
     */
    @PostMapping("/questions/{questionId}/tags/{tagId}")
    public ResponseEntity<?> addTagToQuestion(@PathVariable Long questionId, @PathVariable Long tagId) {
        try {
            boolean success = questionTagService.addTagToQuestion(questionId, tagId);
            if (success) {
                return ResponseEntity.ok(new SuccessResponse("标签添加成功"));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("标签添加失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 从题目中移除单个标签
     */
    @DeleteMapping("/questions/{questionId}/tags/{tagId}")
    public ResponseEntity<?> removeTagFromQuestion(@PathVariable Long questionId, @PathVariable Long tagId) {
        try {
            boolean success = questionTagService.removeTagFromQuestion(questionId, tagId);
            if (success) {
                return ResponseEntity.ok(new SuccessResponse("标签移除成功"));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("标签移除失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 批量为题目添加标签
     */
    @PostMapping("/questions/{questionId}/tags")
    public ResponseEntity<?> addTagsToQuestion(@PathVariable Long questionId, @RequestBody List<Long> tagIds) {
        try {
            boolean success = questionTagService.addTagsToQuestion(questionId, tagIds);
            if (success) {
                return ResponseEntity.ok(new SuccessResponse("标签批量添加成功"));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("标签批量添加失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 批量从题目中移除标签
     */
    @DeleteMapping("/questions/{questionId}/tags")
    public ResponseEntity<?> removeTagsFromQuestion(@PathVariable Long questionId, @RequestBody List<Long> tagIds) {
        try {
            boolean success = questionTagService.removeTagsFromQuestion(questionId, tagIds);
            if (success) {
                return ResponseEntity.ok(new SuccessResponse("标签批量移除成功"));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("标签批量移除失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 设置题目的标签（覆盖所有现有标签）
     */
    @PutMapping("/questions/{questionId}/tags")
    public ResponseEntity<?> setQuestionTags(@PathVariable Long questionId, @RequestBody List<Long> tagIds) {
        try {
            boolean success = questionTagService.setQuestionTags(questionId, tagIds);
            if (success) {
                return ResponseEntity.ok(new SuccessResponse("题目标签设置成功"));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("题目标签设置失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 移除题目的所有标签
     */
    @DeleteMapping("/questions/{questionId}/tags/all")
    public ResponseEntity<?> removeAllTagsFromQuestion(@PathVariable Long questionId) {
        try {
            boolean success = questionTagService.removeAllTagsFromQuestion(questionId);
            if (success) {
                return ResponseEntity.ok(new SuccessResponse("题目所有标签移除成功"));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("题目所有标签移除失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 通过标签名称为题目添加标签
     */
    @PostMapping("/questions/{questionId}/tags/by-name")
    public ResponseEntity<?> addTagToQuestionByName(@PathVariable Long questionId, @RequestBody TagNameRequest request) {
        try {
            boolean success = questionTagService.addTagToQuestionByName(questionId, request.getName());
            if (success) {
                return ResponseEntity.ok(new SuccessResponse("标签添加成功"));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("标签添加失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取题目的所有标签
     */
    @GetMapping("/questions/{questionId}/tags")
    public ResponseEntity<List<TagDTO>> getTagsByQuestionId(@PathVariable Long questionId) {
        List<TagDTO> tags = questionTagService.getTagsByQuestionId(questionId);
        return ResponseEntity.ok(tags);
    }

    /**
     * 获取标签的所有题目
     */
    @GetMapping("/tags/{tagId}/questions")
    public ResponseEntity<List<QuestionDTO>> getQuestionsByTagId(@PathVariable Long tagId) {
        List<QuestionDTO> questions = questionTagService.getQuestionsByTagId(tagId);
        return ResponseEntity.ok(questions);
    }

    /**
     * 根据多个标签查询题目（AND关系 - 题目必须包含所有标签）
     */
    @PostMapping("/questions/by-all-tags")
    public ResponseEntity<List<QuestionDTO>> getQuestionsByAllTags(@RequestBody List<Long> tagIds) {
        List<QuestionDTO> questions = questionTagService.getQuestionsByAllTags(tagIds);
        return ResponseEntity.ok(questions);
    }

    /**
     * 根据多个标签查询题目（OR关系 - 题目包含任一标签）
     */
    @PostMapping("/questions/by-any-tags")
    public ResponseEntity<List<QuestionDTO>> getQuestionsByAnyTags(@RequestBody List<Long> tagIds) {
        List<QuestionDTO> questions = questionTagService.getQuestionsByAnyTags(tagIds);
        return ResponseEntity.ok(questions);
    }

    /**
     * 根据标签名称查询题目（支持模糊搜索）
     */
    @GetMapping("/questions/by-tag-name")
    public ResponseEntity<List<QuestionDTO>> getQuestionsByTagName(@RequestParam String tagName) {
        List<QuestionDTO> questions = questionTagService.getQuestionsByTagName(tagName);
        return ResponseEntity.ok(questions);
    }

    /**
     * 获取最热门的标签
     */
    @GetMapping("/tags/hot")
    public ResponseEntity<List<TagDTO>> getHotTags(@RequestParam(defaultValue = "10") int limit) {
        List<TagDTO> tags = questionTagService.getHotTags(limit);
        return ResponseEntity.ok(tags);
    }

    /**
     * 获取没有关联任何题目的标签
     */
    @GetMapping("/tags/orphan")
    public ResponseEntity<List<TagDTO>> getOrphanTags() {
        List<TagDTO> tags = questionTagService.getOrphanTags();
        return ResponseEntity.ok(tags);
    }

    /**
     * 获取没有任何标签的题目
     */
    @GetMapping("/questions/untagged")
    public ResponseEntity<List<QuestionDTO>> getUntaggedQuestions() {
        List<QuestionDTO> questions = questionTagService.getUntaggedQuestions();
        return ResponseEntity.ok(questions);
    }

    /**
     * 获取题目的标签数量
     */
    @GetMapping("/questions/{questionId}/tags/count")
    public ResponseEntity<Integer> getTagCountByQuestionId(@PathVariable Long questionId) {
        int count = questionTagService.getTagCountByQuestionId(questionId);
        return ResponseEntity.ok(count);
    }

    /**
     * 获取标签的题目数量
     */
    @GetMapping("/tags/{tagId}/questions/count")
    public ResponseEntity<Integer> getQuestionCountByTagId(@PathVariable Long tagId) {
        int count = questionTagService.getQuestionCountByTagId(tagId);
        return ResponseEntity.ok(count);
    }

    /**
     * 统计关联关系总数
     */
    @GetMapping("/relations/count")
    public ResponseEntity<Long> getQuestionTagRelationCount() {
        long count = questionTagService.getQuestionTagRelationCount();
        return ResponseEntity.ok(count);
    }

    /**
     * 检查题目是否已有某个标签
     */
    @GetMapping("/questions/{questionId}/tags/{tagId}/exists")
    public ResponseEntity<Boolean> hasTag(@PathVariable Long questionId, @PathVariable Long tagId) {
        boolean exists = questionTagService.hasTag(questionId, tagId);
        return ResponseEntity.ok(exists);
    }

    /**
     * 清理孤立标签（删除没有关联任何题目的标签）
     */
    @DeleteMapping("/tags/orphan/cleanup")
    public ResponseEntity<?> cleanupOrphanTags() {
        try {
            int deletedCount = questionTagService.cleanupOrphanTags();
            return ResponseEntity.ok(new CleanupResponse("孤立标签清理完成", deletedCount));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 标签名称请求DTO
     */
    public static class TagNameRequest {
        private String name;

        public TagNameRequest() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 清理响应DTO
     */
    public static class CleanupResponse {
        private String message;
        private int deletedCount;

        public CleanupResponse(String message, int deletedCount) {
            this.message = message;
            this.deletedCount = deletedCount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public void setDeletedCount(int deletedCount) {
            this.deletedCount = deletedCount;
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