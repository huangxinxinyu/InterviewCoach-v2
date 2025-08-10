package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.TagDTO;
import com.xinyu.InterviewCoach_v2.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 标签控制层
 */
@RestController
@RequestMapping("/api/tags")
@CrossOrigin(origins = "*") // 允许跨域访问
public class TagController {

    @Autowired
    private TagService tagService;

    /**
     * 创建新标签
     */
    @PostMapping
    public ResponseEntity<?> createTag(@RequestBody TagDTO tagDTO) {
        try {
            TagDTO createdTag = tagService.createTag(tagDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 根据ID查询标签
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTagById(@PathVariable Long id) {
        Optional<TagDTO> tag = tagService.getTagById(id);
        if (tag.isPresent()) {
            return ResponseEntity.ok(tag.get());
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
            return ResponseEntity.ok(tag.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 查询所有标签
     */
    @GetMapping
    public ResponseEntity<List<TagDTO>> getAllTags() {
        List<TagDTO> tags = tagService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    /**
     * 根据关键词搜索标签
     */
    @GetMapping("/search")
    public ResponseEntity<List<TagDTO>> searchTags(@RequestParam String keyword) {
        List<TagDTO> tags = tagService.searchTags(keyword);
        return ResponseEntity.ok(tags);
    }

    /**
     * 分页查询标签
     */
    @GetMapping("/page")
    public ResponseEntity<PageResponse<TagDTO>> getTagsWithPagination(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<TagDTO> tags = tagService.getTagsWithPagination(page, size);
        long totalCount = tagService.getTagCount();
        int totalPages = (int) Math.ceil((double) totalCount / size);

        PageResponse<TagDTO> response = new PageResponse<>(
                tags, page, size, totalCount, totalPages
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTag(@PathVariable Long id, @RequestBody TagDTO tagDTO) {
        try {
            TagDTO updatedTag = tagService.updateTag(id, tagDTO);
            return ResponseEntity.ok(updatedTag);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
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
                return ResponseEntity.ok(new SuccessResponse("标签删除成功"));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("删除标签失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取标签总数
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getTagCount() {
        long count = tagService.getTagCount();
        return ResponseEntity.ok(count);
    }

    /**
     * 根据关键词统计标签数量
     */
    @GetMapping("/count/search")
    public ResponseEntity<Long> getTagCountByKeyword(@RequestParam String keyword) {
        long count = tagService.getTagCountByKeyword(keyword);
        return ResponseEntity.ok(count);
    }

    /**
     * 检查标签是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkTagExists(@RequestParam String name) {
        boolean exists = tagService.tagExists(name);
        return ResponseEntity.ok(exists);
    }

    /**
     * 获取最常用的标签
     */
    @GetMapping("/most-used")
    public ResponseEntity<List<TagDTO>> getMostUsedTags(
            @RequestParam(defaultValue = "10") int limit) {
        List<TagDTO> tags = tagService.getMostUsedTags(limit);
        return ResponseEntity.ok(tags);
    }

    /**
     * 获取未使用的标签
     */
    @GetMapping("/unused")
    public ResponseEntity<List<TagDTO>> getUnusedTags() {
        List<TagDTO> tags = tagService.getUnusedTags();
        return ResponseEntity.ok(tags);
    }

    /**
     * 根据题目ID查询相关标签
     */
    @GetMapping("/by-question/{questionId}")
    public ResponseEntity<List<TagDTO>> getTagsByQuestionId(@PathVariable Long questionId) {
        List<TagDTO> tags = tagService.getTagsByQuestionId(questionId);
        return ResponseEntity.ok(tags);
    }

    /**
     * 批量创建或获取标签
     */
    @PostMapping("/batch")
    public ResponseEntity<?> createOrGetTags(@RequestBody List<String> names) {
        try {
            List<TagDTO> tags = tagService.createOrGetTags(names);
            return ResponseEntity.ok(tags);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
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