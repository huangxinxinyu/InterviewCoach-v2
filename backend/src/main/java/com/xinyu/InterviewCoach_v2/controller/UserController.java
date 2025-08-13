package com.xinyu.InterviewCoach_v2.controller;

import com.xinyu.InterviewCoach_v2.dto.UserDTO;
import com.xinyu.InterviewCoach_v2.dto.request.auth.LoginRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.request.auth.RegisterRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.request.auth.TokenRequestDTO;
import com.xinyu.InterviewCoach_v2.dto.response.auth.LoginResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.auth.RegisterResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.auth.TokenValidationResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiErrorResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.common.ApiSuccessResponseDTO;
import com.xinyu.InterviewCoach_v2.enums.UserRole;
import com.xinyu.InterviewCoach_v2.service.AuthService;
import com.xinyu.InterviewCoach_v2.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 用户控制层 - 重构后使用统一的DTO
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            // 验证密码是否一致
            if (!request.isPasswordMatch()) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("密码确认不一致", "PASSWORD_MISMATCH"));
            }

            // 创建用户DTO
            UserDTO userDTO = new UserDTO(request.getEmail(), request.getPassword());
            UserDTO createdUser = userService.createUser(userDTO);

            // 注册成功后自动登录
            AuthService.LoginResult loginResult = authService.login(
                    request.getEmail(),
                    request.getPassword()
            );

            if (loginResult.isSuccess()) {
                RegisterResponseDTO response = RegisterResponseDTO.builder()
                        .success(true)
                        .message("注册成功")
                        .token(loginResult.getToken())
                        .user(loginResult.getUser());

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiErrorResponseDTO("注册成功但登录失败", "LOGIN_AFTER_REGISTER_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "REGISTER_FAILED"));
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
     * 查询所有用户
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponseDTO<List<UserDTO>>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(users));
    }

    /**
     * 根据角色查询用户
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<ApiSuccessResponseDTO<List<UserDTO>>> getUsersByRole(@PathVariable UserRole role) {
        List<UserDTO> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(users));
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserDTO request) {
        try {
            UserDTO user = userService.updateUser(id, request);
            return ResponseEntity.ok(new ApiSuccessResponseDTO<>(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "UPDATE_USER_FAILED"));
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            boolean deleted = userService.deleteUser(id);
            if (deleted) {
                return ResponseEntity.ok(new ApiSuccessResponseDTO<>("用户删除成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponseDTO("删除用户失败", "DELETE_USER_FAILED"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponseDTO(e.getMessage(), "DELETE_USER_ERROR"));
        }
    }

    /**
     * 检查邮箱是否存在
     */
    @GetMapping("/check-email/{email}")
    public ResponseEntity<ApiSuccessResponseDTO<Boolean>> checkEmailExists(@PathVariable String email) {
        boolean exists = userService.emailExists(email);
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(exists));
    }

    /**
     * 获取用户总数
     */
    @GetMapping("/count")
    public ResponseEntity<ApiSuccessResponseDTO<Long>> getUserCount() {
        long count = userService.getUserCount();
        return ResponseEntity.ok(new ApiSuccessResponseDTO<>(count));
    }
}