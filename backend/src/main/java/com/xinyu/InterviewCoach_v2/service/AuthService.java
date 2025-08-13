package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.UserDTO;
import com.xinyu.InterviewCoach_v2.dto.response.auth.LoginResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.auth.TokenValidationResponseDTO;
import com.xinyu.InterviewCoach_v2.entity.User;
import com.xinyu.InterviewCoach_v2.util.DTOConverter;
import com.xinyu.InterviewCoach_v2.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 认证服务 - 完全使用新的DTO结构
 */
@Service
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private DTOConverter dtoConverter;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录 - 使用新的响应DTO
     */
    public LoginResponseDTO loginWithDTO(String email, String password) {
        Optional<User> userOpt = userService.authenticate(email, password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 生成JWT token
            String token = jwtUtil.generateToken(
                    user.getEmail(),
                    user.getRole().name(),
                    user.getId()
            );

            // 转换为DTO并清除敏感信息
            UserDTO userDTO = dtoConverter.convertToUserDTO(user).clearSensitiveInfo();

            return LoginResponseDTO.builder()
                    .success(true)
                    .message("登录成功")
                    .token(token)
                    .user(userDTO);
        } else {
            return LoginResponseDTO.builder()
                    .success(false)
                    .message("邮箱或密码错误");
        }
    }

    /**
     * 验证Token并返回详细信息
     */
    public TokenValidationResponseDTO validateTokenWithDTO(String token) {
        try {
            if (jwtUtil.isTokenValid(token)) {
                String email = jwtUtil.getUsernameFromToken(token);
                Optional<UserDTO> user = userService.getUserByEmail(email);

                if (user.isPresent()) {
                    return TokenValidationResponseDTO.builder()
                            .valid(true)
                            .message("Token有效")
                            .user(user.get().clearSensitiveInfo());
                }
            }

            return TokenValidationResponseDTO.builder()
                    .valid(false)
                    .message("Token无效或已过期");

        } catch (Exception e) {
            return TokenValidationResponseDTO.builder()
                    .valid(false)
                    .message("Token验证失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录 - 保持原有方法兼容性
     */
    public LoginResult login(String email, String password) {
        LoginResponseDTO newResponse = loginWithDTO(email, password);

        return new LoginResult(
                newResponse.isSuccess(),
                newResponse.getMessage(),
                newResponse.getToken(),
                newResponse.getUser()
        );
    }

    /**
     * 登录结果内部类 - 保持向后兼容
     */
    public static class LoginResult {
        private boolean success;
        private String message;
        private String token;
        private UserDTO user;

        public LoginResult(boolean success, String message, String token, UserDTO user) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.user = user;
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getToken() {
            return token;
        }

        public UserDTO getUser() {
            return user;
        }

        // Setters
        public void setSuccess(boolean success) {
            this.success = success;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public void setUser(UserDTO user) {
            this.user = user;
        }
    }
}