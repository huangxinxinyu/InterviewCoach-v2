package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.UserDTO;
import com.xinyu.InterviewCoach_v2.dto.response.auth.LoginResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.auth.RegisterResponseDTO;
import com.xinyu.InterviewCoach_v2.dto.response.auth.TokenValidationResponseDTO;
import com.xinyu.InterviewCoach_v2.entity.User;
import com.xinyu.InterviewCoach_v2.util.DTOConverter;
import com.xinyu.InterviewCoach_v2.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 认证服务 - 重构支持邮箱验证码注册
 */
@Service
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private DTOConverter dtoConverter;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 发送注册验证码
     * @param email 用户邮箱
     * @return 是否成功发送
     */
    public boolean sendRegisterVerificationCode(String email) {
        try {
            // 检查邮箱是否已存在
            if (userService.emailExists(email)) {
                throw new RuntimeException("邮箱已被注册");
            }

            // 发送验证码
            emailVerificationService.sendVerificationCode(email);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("发送验证码失败: " + e.getMessage());
        }
    }

    /**
     * 验证邮箱验证码并完成注册
     * @param email 邮箱地址
     * @param code 验证码
     * @param password 密码
     * @return 注册响应
     */
    public RegisterResponseDTO registerWithEmailVerification(String email, String code, String password) {
        try {
            // 验证验证码
            if (!emailVerificationService.verifyCode(email, code)) {
                return RegisterResponseDTO.builder()
                        .success(false)
                        .message("验证码错误或已过期");
            }

            // 再次检查邮箱是否已存在
            if (userService.emailExists(email)) {
                return RegisterResponseDTO.builder()
                        .success(false)
                        .message("邮箱已被注册");
            }

            // 创建用户
            UserDTO userDTO = new UserDTO(email, password);
            UserDTO createdUser = userService.createUser(userDTO);

            // 自动登录
            String token = jwtUtil.generateToken(
                    createdUser.getEmail(),
                    createdUser.getRole().name(),
                    createdUser.getId()
            );

            return RegisterResponseDTO.builder()
                    .success(true)
                    .message("注册成功")
                    .token(token)
                    .user(createdUser.clearSensitiveInfo());

        } catch (Exception e) {
            return RegisterResponseDTO.builder()
                    .success(false)
                    .message("注册失败: " + e.getMessage());
        }
    }

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
                newResponse.getToken(),
                newResponse.getUser(),
                newResponse.getMessage()
        );
    }

    /**
     * 兼容性内部类 - LoginResult
     */
    public static class LoginResult {
        private boolean success;
        private String token;
        private UserDTO user;
        private String message;

        public LoginResult(boolean success, String token, UserDTO user, String message) {
            this.success = success;
            this.token = token;
            this.user = user;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getToken() {
            return token;
        }

        public UserDTO getUser() {
            return user;
        }

        public String getMessage() {
            return message;
        }
    }
}