package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.UserDTO;
import com.xinyu.InterviewCoach_v2.dto.request.admin.BatchImportRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiErrorResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiSuccessResponseDTO;
import com.xinyu.InterviewCoach_v2.enums.UserRole;
import com.xinyu.InterviewCoach_v2.service.BatchImportService;
import com.xinyu.InterviewCoach_v2.service.UserService;
import com.xinyu.InterviewCoach_v2.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * 批量导入题目 - 简化版本
     */
    @PostMapping("/questions/batch-import")
    public ResponseEntity<?> batchImportQuestions(
            @Valid @RequestBody BatchImportRequestDTO request,
            HttpServletRequest httpRequest) {

        try {
            // 验证管理员权限
            if (!isAdmin(httpRequest)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiErrorResponseDTO("权限不足，需要管理员权限", "INSUFFICIENT_PERMISSION"));
            }

            // 基本验证
            if (request.getQuestions().size() > 500) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("单次导入题目数量不能超过500个", "TOO_MANY_QUESTIONS"));
            }

            // 执行批量导入
            Map<String, Object> result = batchImportService.batchImportQuestions(request);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>(result.get("message").toString(), result));
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

            return userOpt.isPresent() && UserRole.ADMIN.name().equals(userOpt.get().getRole());
        } catch (Exception e) {
            return false;
        }
    }
}