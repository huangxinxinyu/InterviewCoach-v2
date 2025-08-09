package com.xinyu.InterviewCoach_v2.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类
 * 使用BCrypt算法进行密码哈希
 */
@Component
public class PasswordUtil {

    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordUtil() {
        // 10次哈希运算
        this.passwordEncoder = new BCryptPasswordEncoder(10);
    }

    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码（包含盐值）
     */
    public String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 验证密码
     * @param rawPassword 用户输入的原始密码
     * @param encodedPassword 数据库中存储的加密密码
     * @return 密码是否匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        // 这里可以添加更复杂的密码强度检查逻辑
        // 比如：包含大小写字母、数字、特殊字符等
        return true;
    }
}