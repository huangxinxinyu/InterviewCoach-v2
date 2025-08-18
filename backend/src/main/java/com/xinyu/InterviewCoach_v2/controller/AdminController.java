package com.xinyu.InterviewCoach_v2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinyu.InterviewCoach_v2.dto.*;
import com.xinyu.InterviewCoach_v2.dto.request.admin.BatchImportRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiErrorResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiSuccessResponseDTO;
import com.xinyu.InterviewCoach_v2.enums.UserRole;
import com.xinyu.InterviewCoach_v2.service.*;
import com.xinyu.InterviewCoach_v2.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private BatchImportService batchImportService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 通过JSON文件批量导入题目
     */
    @PostMapping("/questions/batch-import-file")
    public ResponseEntity<?> batchImportQuestionsFromFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        try {
            // 验证管理员权限
            if (!isAdmin(httpRequest)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiErrorResponseDTO("权限不足，需要管理员权限", "INSUFFICIENT_PERMISSION"));
            }

            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("文件不能为空", "EMPTY_FILE"));
            }

            // 验证文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".json")) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("只支持JSON文件", "INVALID_FILE_TYPE"));
            }

            // 验证文件大小 (限制10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("文件大小不能超过10MB", "FILE_TOO_LARGE"));
            }

            // 解析JSON文件
            BatchImportRequestDTO request;
            try {
                String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
                ObjectMapper objectMapper = new ObjectMapper();
                request = objectMapper.readValue(jsonContent, BatchImportRequestDTO.class);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("JSON文件格式错误: " + e.getMessage(), "INVALID_JSON"));
            }

            // 基本验证
            if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("文件中没有题目数据", "NO_QUESTIONS"));
            }

            if (request.getQuestions().size() > 500) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("单次导入题目数量不能超过500个", "TOO_MANY_QUESTIONS"));
            }

            // 执行批量导入
            Map<String, Object> result = batchImportService.batchImportQuestions(request);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>(
                        "文件导入完成: " + result.get("message").toString(),
                        result
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiErrorResponseDTO(result.get("message").toString(), "BATCH_IMPORT_FAILED"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponseDTO("服务器内部错误: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

    /**
     * 验证是否为管理员
     */
    private boolean isAdmin(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return false;
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return false;
            }

            String email = jwtUtil.getUsernameFromToken(token);
            Optional<UserDTO> userOpt = userService.getUserByEmail(email);

            return userOpt.isPresent() && UserRole.ADMIN.equals(userOpt.get().getRole());
        } catch (Exception e) {
            return false;
        }
    }
}