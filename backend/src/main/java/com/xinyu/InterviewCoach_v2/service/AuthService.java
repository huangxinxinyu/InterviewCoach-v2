package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.UserDTO;
import com.xinyu.InterviewCoach_v2.entity.User;
import com.xinyu.InterviewCoach_v2.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 认证服务
 */
@Service
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    public LoginResult login(String email, String password) {
        Optional<User> userOpt = userService.authenticate(email, password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 生成JWT token
            String token = jwtUtil.generateToken(
                    user.getEmail(),
                    user.getRole().name(),
                    user.getId()
            );

            // 转换为DTO
            UserDTO userDTO = new UserDTO(
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );

            return new LoginResult(true, "登录成功", token, userDTO);
        } else {
            return new LoginResult(false, "邮箱或密码错误", null, null);
        }
    }

    /**
     * 验证token
     */
    public boolean validateToken(String token) {
        return jwtUtil.isTokenValid(token);
    }

    /**
     * 从token获取用户信息
     */
    public Optional<UserDTO> getUserFromToken(String token) {
        try {
            String email = jwtUtil.getUsernameFromToken(token);
            return userService.getUserByEmail(email);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 登录结果内部类
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