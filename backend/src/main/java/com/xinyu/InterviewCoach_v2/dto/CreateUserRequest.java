package com.xinyu.InterviewCoach_v2.dto;

/**
 * 创建用户请求DTO
 * 注意：只能创建普通用户，管理员需要通过数据库直接初始化
 */
public class CreateUserRequest {

    private String email;
    private String password;

    public CreateUserRequest() {}

    public CreateUserRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

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
        return "CreateUserRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}
