package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.TemplateDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiErrorResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiSuccessResponseDTO;
import com.xinyu.InterviewCoach_v2.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 模板控制层
 */
@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "*")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    /**
     * 创建新模板
     */
    @PostMapping
    public ResponseEntity<?> createTemplate(@Valid @RequestBody TemplateDTO templateDTO) {
        try {
            TemplateDTO createdTemplate = templateService.createTemplate(templateDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiSuccessResponseDTO<>("模板创建成功", createdTemplate));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "CREATE_TEMPLATE_FAILED"));
        }
    }

    /**
     * 根据ID查询模板
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplateById(@PathVariable Long id) {
        Optional<TemplateDTO> template = templateService.getTemplateById(id);
        if (template.isPresent()) {
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(template.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 查询所有模板
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponseDTO<List<TemplateDTO>>> getAllTemplates() {
        List<TemplateDTO> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(templates));
    }

    /**
     * 根据关键词搜索模板
     */
    @GetMapping("/search")
    public ResponseEntity<ApiSuccessResponseDTO<List<TemplateDTO>>> searchTemplates(@RequestParam String keyword) {
        List<TemplateDTO> templates = templateService.searchTemplates(keyword);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(templates));
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateDTO templateDTO) {
        try {
            TemplateDTO updatedTemplate = templateService.updateTemplate(id, templateDTO);
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>("模板更新成功", updatedTemplate));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "UPDATE_TEMPLATE_FAILED"));
        }
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        try {
            boolean deleted = templateService.deleteTemplate(id);
            if (deleted) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("模板删除成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("删除模板失败", "DELETE_TEMPLATE_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "DELETE_TEMPLATE_ERROR"));
        }
    }

    /**
     * 获取解析后的模板
     */
    @GetMapping("/{id}/parsed")
    public ResponseEntity<?> getParsedTemplate(@PathVariable Long id) {
        try {
            TemplateDTO parsedTemplate = templateService.parseTemplateContent(id);
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(parsedTemplate));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "PARSE_TEMPLATE_FAILED"));
        }
    }

    /**
     * 根据名称查询模板
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<?> getTemplateByName(@PathVariable String name) {
        Optional<TemplateDTO> template = templateService.getTemplateByName(name);
        if (template.isPresent()) {
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(template.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取模板总数
     */
    @GetMapping("/count")
    public ResponseEntity<ApiSuccessResponseDTO<Long>> getTemplateCount() {
        long count = templateService.getTemplateCount();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(count));
    }

    /**
     * 检查模板是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<ApiSuccessResponseDTO<Boolean>> checkTemplateExists(@RequestParam String name) {
        boolean exists = templateService.templateExists(name);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(exists));
    }

    /**
     * 获取最新模板
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiSuccessResponseDTO<List<TemplateDTO>>> getLatestTemplates(
            @RequestParam(defaultValue = "10") int limit) {
        List<TemplateDTO> templates = templateService.getLatestTemplates(limit);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(templates));
    }
}