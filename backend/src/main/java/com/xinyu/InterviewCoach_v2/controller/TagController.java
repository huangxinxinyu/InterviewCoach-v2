package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.TagDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiErrorResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiSuccessResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.PageResponseDTO;
import com.xinyu.InterviewCoach_v2.service.TagService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 标签控制层 - 重构后使用统一的DTO
 */
@RestController
@RequestMapping("/api/tags")
@CrossOrigin(origins = "*")
public class TagController {

    @Autowired
    private TagService tagService;

    /**
     * 创建新标签
     */
    @PostMapping
    public ResponseEntity<?> createTag(@Valid @RequestBody TagDTO tagDTO) {
        try {
            TagDTO createdTag = tagService.createTag(tagDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiSuccessResponseDTO<>("标签创建成功", createdTag));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "CREATE_TAG_FAILED"));
        }
    }

    /**
     * 根据ID查询标签
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTagById(@PathVariable Long id) {
        Optional<TagDTO> tag = tagService.getTagById(id);
        if (tag.isPresent()) {
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(tag.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根据名称查询标签
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<?> getTagByName(@PathVariable String name) {
        Optional<TagDTO> tag = tagService.getTagByName(name);
        if (tag.isPresent()) {
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(tag.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 查询所有标签
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponseDTO<List<TagDTO>>> getAllTags() {
        List<TagDTO> tags = tagService.getAllTags();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(tags));
    }

    /**
     * 根据关键词搜索标签
     */
    @GetMapping("/search")
    public ResponseEntity<ApiSuccessResponseDTO<List<TagDTO>>> searchTags(@RequestParam String keyword) {
        List<TagDTO> tags = tagService.searchTags(keyword);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(tags));
    }

    /**
     * 分页查询标签
     */
    @GetMapping("/page")
    public ResponseEntity<ApiSuccessResponseDTO<PageResponseDTO<TagDTO>>> getTagsWithPagination(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<TagDTO> tags = tagService.getTagsWithPagination(page, size);
        long totalCount = tagService.getTagCount();
        int totalPages = (int) Math.ceil((double) totalCount / size);

        PageResponseDTO<TagDTO> pageResponse = PageResponseDTO.<TagDTO>builder()
                .content(tags)
                .currentPage(page)
                .pageSize(size)
                .totalElements(totalCount)
                .totalPages(totalPages);

        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(pageResponse));
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTag(@PathVariable Long id, @Valid @RequestBody TagDTO tagDTO) {
        try {
            TagDTO updatedTag = tagService.updateTag(id, tagDTO);
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>("标签更新成功", updatedTag));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "UPDATE_TAG_FAILED"));
        }
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTag(@PathVariable Long id) {
        try {
            boolean deleted = tagService.deleteTag(id);
            if (deleted) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("标签删除成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("删除标签失败", "DELETE_TAG_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "DELETE_TAG_ERROR"));
        }
    }

    /**
     * 获取标签总数
     */
    @GetMapping("/count")
    public ResponseEntity<ApiSuccessResponseDTO<Long>> getTagCount() {
        long count = tagService.getTagCount();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(count));
    }

    /**
     * 根据关键词统计标签数量
     */
    @GetMapping("/count/search")
    public ResponseEntity<ApiSuccessResponseDTO<Long>> getTagCountByKeyword(@RequestParam String keyword) {
        long count = tagService.getTagCountByKeyword(keyword);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(count));
    }

    /**
     * 检查标签是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<ApiSuccessResponseDTO<Boolean>> checkTagExists(@RequestParam String name) {
        boolean exists = tagService.tagExists(name);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(exists));
    }

    /**
     * 获取最常用的标签
     */
    @GetMapping("/most-used")
    public ResponseEntity<ApiSuccessResponseDTO<List<TagDTO>>> getMostUsedTags(
            @RequestParam(defaultValue = "10") int limit) {
        List<TagDTO> tags = tagService.getMostUsedTags(limit);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(tags));
    }

    /**
     * 获取未使用的标签
     */
    @GetMapping("/unused")
    public ResponseEntity<ApiSuccessResponseDTO<List<TagDTO>>> getUnusedTags() {
        List<TagDTO> tags = tagService.getUnusedTags();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(tags));
    }

    /**
     * 根据题目ID查询相关标签
     */
    @GetMapping("/by-question/{questionId}")
    public ResponseEntity<ApiSuccessResponseDTO<List<TagDTO>>> getTagsByQuestionId(@PathVariable Long questionId) {
        List<TagDTO> tags = tagService.getTagsByQuestionId(questionId);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(tags));
    }

    /**
     * 批量创建或获取标签
     */
    @PostMapping("/batch")
    public ResponseEntity<?> createOrGetTags(@RequestBody List<String> names) {
        try {
            List<TagDTO> tags = tagService.createOrGetTags(names);
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>("标签批量处理成功", tags));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "BATCH_TAG_FAILED"));
        }
    }
}