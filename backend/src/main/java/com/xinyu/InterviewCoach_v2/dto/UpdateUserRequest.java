package com.xinyu.InterviewCoach_v2.dto;

/**
 * 更新用户请求DTO
 * 只允许更新邮箱和密码，不允许更改角色
 */
class UpdateUserRequest {

    private String email;
    private String password;

    public UpdateUserRequest() {}

    // Getters and Setters
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

    @Override
    public String toString() {
        return "UpdateUserRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}