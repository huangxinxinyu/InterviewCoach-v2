package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.UserDTO;
import com.xinyu.InterviewCoach_v2.dto.request.auth.LoginRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.request.auth.RegisterRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.request.auth.TokenRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.request.email.SendVerificationCodeRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.response.auth.LoginResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.auth.RegisterResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.auth.TokenValidationResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiErrorResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiSuccessResponseDTO;
import com.xinyu.InterviewCoach_v2.service.AuthService;
import com.xinyu.InterviewCoach_v2.service.UserService;
import com.xinyu.InterviewCoach_v2.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 用户控制层 - 重构支持邮箱验证码注册
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 发送注册验证码
     */
    @PostMapping("/send-register-code")
    public ResponseEntity<?> sendRegisterCode(@Valid @RequestBody SendVerificationCodeRequestDTO request) {
        try {
            boolean sent = authService.sendRegisterVerificationCode(request.getEmail());

            if (sent) {
                return ResponseEntity.ok(
                        new ApiSuccessResponseDTO<>("验证码发送成功，请查收邮件")
                );
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiErrorResponseDTO("验证码发送失败", "SEND_CODE_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "SEND_CODE_ERROR"));
        }
    }

    /**
     * 用户注册 - 重构支持验证码
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            // 验证密码是否一致
            if (!request.isPasswordMatch()) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("密码确认不一致", "PASSWORD_MISMATCH"));
            }

            // 使用验证码注册
            RegisterResponseDTO response = authService.registerWithEmailVerification(
                    request.getEmail(),
                    request.getCode(),
                    request.getPassword()
            );

            if (response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO(response.getMessage(), "REGISTER_FAILED"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO("注册失败: " + e.getMessage(), "REGISTER_ERROR"));
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO result = authService.loginWithDTO(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponseDTO(result.getMessage(), "LOGIN_FAILED"));
        }
    }

    /**
     * 验证token
     */
    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@Valid @RequestBody TokenRequestDTO tokenRequest) {
        TokenValidationResponseDTO result = authService.validateTokenWithDTO(tokenRequest.getToken());

        if (result.isValid()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<UserDTO> user = userService.getUserById(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(user.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根据邮箱查询用户
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        Optional<UserDTO> user = userService.getUserByEmail(email);
        if (user.isPresent()) {
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(user.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            String email = jwtUtil.getUsernameFromToken(token);

            Optional<UserDTO> user = userService.getUserByEmail(email);
            if (user.isPresent()) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>(user.get().clearSensitiveInfo()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiErrorResponseDTO("用户不存在", "USER_NOT_FOUND"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponseDTO("Token无效", "INVALID_TOKEN"));
        }
    }
}