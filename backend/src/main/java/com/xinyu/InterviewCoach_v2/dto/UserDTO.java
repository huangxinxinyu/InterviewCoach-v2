package com.xinyu.InterviewCoach_v2.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinyu.InterviewCoach_v2.entity.User;
import com.xinyu.InterviewCoach_v2.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 用户数据传输对象
 * 整合了用户信息展示、登录请求、注册请求、更新请求等功能
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // 只包含非null字段
public class UserDTO {
    private Long id;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;

    private UserRole role;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private String token;
    private boolean success;
    private String message;

    public UserDTO() {}

    /**
     * 完整信息构造方法（用于查询用户信息）
     */
    public UserDTO(Long id, String email, UserRole role, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 登录请求构造方法
     */
    public UserDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * 登录响应构造方法
     */
    public UserDTO(boolean success, String message, String token, Long id, String email, UserRole role, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.id = id;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // helper methods

    /**
     * 检查是否为管理员
     */
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }

    /**
     * 检查是否为普通用户
     */
    public boolean isUser() {
        return UserRole.USER.equals(this.role);
    }

    /**
     * 获取角色描述
     */
    public String getRoleDescription() {
        return role != null ? role.getDescription() : "未知";
    }

    /**
     * 获取用户显示名称（使用邮箱前缀）
     */
    public String getDisplayName() {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }
        return email;
    }

    /**
     * 检查是否需要更新密码（用于更新请求）
     */
    public boolean shouldUpdatePassword() {
        return password != null && !password.trim().isEmpty();
    }

    /**
     * 清除敏感信息（密码）
     */
    public UserDTO clearSensitiveInfo() {
        this.password = null;
        return this;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserDTO userDTO = (UserDTO) obj;
        return id != null ? id.equals(userDTO.id) : userDTO.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}