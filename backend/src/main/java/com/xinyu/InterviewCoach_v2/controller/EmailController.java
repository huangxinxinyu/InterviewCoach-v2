// EmailController.java
package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.request.email.SendVerificationCodeRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.request.email.VerifyEmailRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiErrorResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiSuccessResponseDTO;
import com.xinyu.InterviewCoach_v2.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*")
public class EmailController {

    @Autowired
    private EmailVerificationService emailVerificationService;

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequestDTO request) {
        try {
            emailVerificationService.sendVerificationCode(request.getEmail());
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>("验证码发送成功", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO("验证码发送失败：" + e.getMessage(), "SEND_CODE_FAILED"));
        }
    }

    /**
     * 验证邮箱验证码
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequestDTO request) {
        try {
            boolean isValid = emailVerificationService.verifyCode(request.getEmail(), request.getCode());

            if (isValid) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("验证成功", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("验证码错误或已过期", "INVALID_CODE"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO("验证失败：" + e.getMessage(), "VERIFY_FAILED"));
        }
    }
}