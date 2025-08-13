package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.QuestionDTO;
import com.xinyu.InterviewCoach_v2.dto.TagDTO;
import com.xinyu.InterviewCoach_v2.dto.request.tag.TagNameRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiErrorResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiSuccessResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.tag.CleanupResponseDTO;
import com.xinyu.InterviewCoach_v2.service.QuestionTagService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目标签关联控制层 - 重构后使用统一的DTO
 */
@RestController
@RequestMapping("/api/question-tags")
@CrossOrigin(origins = "*")
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
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("标签添加成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("标签添加失败", "ADD_TAG_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "ADD_TAG_ERROR"));
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
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("标签移除成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("标签移除失败", "REMOVE_TAG_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "REMOVE_TAG_ERROR"));
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
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("标签批量添加成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("标签批量添加失败", "BATCH_ADD_TAGS_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "BATCH_ADD_TAGS_ERROR"));
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
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("标签批量移除成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("标签批量移除失败", "BATCH_REMOVE_TAGS_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "BATCH_REMOVE_TAGS_ERROR"));
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
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("题目标签设置成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("题目标签设置失败", "SET_QUESTION_TAGS_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "SET_QUESTION_TAGS_ERROR"));
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
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("题目所有标签移除成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("题目所有标签移除失败", "REMOVE_ALL_TAGS_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "REMOVE_ALL_TAGS_ERROR"));
        }
    }

    /**
     * 通过标签名称为题目添加标签
     */
    @PostMapping("/questions/{questionId}/tags/by-name")
    public ResponseEntity<?> addTagToQuestionByName(@PathVariable Long questionId,
                                                    @Valid @RequestBody TagNameRequestDTO request) {
        try {
            boolean success = questionTagService.addTagToQuestionByName(questionId, request.getName());
            if (success) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("标签添加成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("标签添加失败", "ADD_TAG_BY_NAME_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "ADD_TAG_BY_NAME_ERROR"));
        }
    }

    /**
     * 获取题目的所有标签
     */
    @GetMapping("/questions/{questionId}/tags")
    public ResponseEntity<ApiSuccessResponseDTO<List<TagDTO>>> getTagsByQuestionId(@PathVariable Long questionId) {
        List<TagDTO> tags = questionTagService.getTagsByQuestionId(questionId);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(tags));
    }

    /**
     * 获取标签的所有题目
     */
    @GetMapping("/tags/{tagId}/questions")
    public ResponseEntity<ApiSuccessResponseDTO<List<QuestionDTO>>> getQuestionsByTagId(@PathVariable Long tagId) {
        List<QuestionDTO> questions = questionTagService.getQuestionsByTagId(tagId);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(questions));
    }

    /**
     * 根据多个标签查询题目（AND关系 - 题目必须包含所有标签）
     */
    @PostMapping("/questions/by-all-tags")
    public ResponseEntity<ApiSuccessResponseDTO<List<QuestionDTO>>> getQuestionsByAllTags(@RequestBody List<Long> tagIds) {
        List<QuestionDTO> questions = questionTagService.getQuestionsByAllTags(tagIds);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(questions));
    }

    /**
     * 根据多个标签查询题目（OR关系 - 题目包含任一标签）
     */
    @PostMapping("/questions/by-any-tags")
    public ResponseEntity<ApiSuccessResponseDTO<List<QuestionDTO>>> getQuestionsByAnyTags(@RequestBody List<Long> tagIds) {
        List<QuestionDTO> questions = questionTagService.getQuestionsByAnyTags(tagIds);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(questions));
    }

    /**
     * 根据标签名称查询题目（支持模糊搜索）
     */
    @GetMapping("/questions/by-tag-name")
    public ResponseEntity<ApiSuccessResponseDTO<List<QuestionDTO>>> getQuestionsByTagName(@RequestParam String tagName) {
        List<QuestionDTO> questions = questionTagService.getQuestionsByTagName(tagName);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(questions));
    }

    /**
     * 获取最热门的标签
     */
    @GetMapping("/tags/hot")
    public ResponseEntity<ApiSuccessResponseDTO<List<TagDTO>>> getHotTags(@RequestParam(defaultValue = "10") int limit) {
        List<TagDTO> tags = questionTagService.getHotTags(limit);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(tags));
    }

    /**
     * 获取没有关联任何题目的标签
     */
    @GetMapping("/tags/orphan")
    public ResponseEntity<ApiSuccessResponseDTO<List<TagDTO>>> getOrphanTags() {
        List<TagDTO> tags = questionTagService.getOrphanTags();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(tags));
    }

    /**
     * 获取没有任何标签的题目
     */
    @GetMapping("/questions/untagged")
    public ResponseEntity<ApiSuccessResponseDTO<List<QuestionDTO>>> getUntaggedQuestions() {
        List<QuestionDTO> questions = questionTagService.getUntaggedQuestions();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(questions));
    }

    /**
     * 获取题目的标签数量
     */
    @GetMapping("/questions/{questionId}/tags/count")
    public ResponseEntity<ApiSuccessResponseDTO<Integer>> getTagCountByQuestionId(@PathVariable Long questionId) {
        int count = questionTagService.getTagCountByQuestionId(questionId);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(count));
    }

    /**
     * 获取标签的题目数量
     */
    @GetMapping("/tags/{tagId}/questions/count")
    public ResponseEntity<ApiSuccessResponseDTO<Integer>> getQuestionCountByTagId(@PathVariable Long tagId) {
        int count = questionTagService.getQuestionCountByTagId(tagId);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(count));
    }

    /**
     * 统计关联关系总数
     */
    @GetMapping("/relations/count")
    public ResponseEntity<ApiSuccessResponseDTO<Long>> getQuestionTagRelationCount() {
        long count = questionTagService.getQuestionTagRelationCount();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(count));
    }

    /**
     * 检查题目是否已有某个标签
     */
    @GetMapping("/questions/{questionId}/tags/{tagId}/exists")
    public ResponseEntity<ApiSuccessResponseDTO<Boolean>> hasTag(@PathVariable Long questionId, @PathVariable Long tagId) {
        boolean exists = questionTagService.hasTag(questionId, tagId);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(exists));
    }

    /**
     * 清理孤立标签（删除没有关联任何题目的标签）
     */
    @DeleteMapping("/tags/orphan/cleanup")
    public ResponseEntity<?> cleanupOrphanTags() {
        try {
            int deletedCount = questionTagService.cleanupOrphanTags();
            CleanupResponseDTO response = CleanupResponseDTO.builder()
                    .message("孤立标签清理完成")
                    .deletedCount(deletedCount)
                    .operation("cleanup_orphan_tags");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "CLEANUP_ORPHAN_TAGS_ERROR"));
        }
    }
}